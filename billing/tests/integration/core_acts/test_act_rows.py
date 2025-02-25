# -*- coding: utf-8 -*-

import contextlib
import re
from unittest import mock

import pytest
import hamcrest

from yt.wrapper import (
    ypath_join,
)

from billing.log_tariffication.py.lib.constants import (
    LOG_INTERVAL_KEY,
    ACT_SEQUENCE_POS_KEY,
    RUN_ID_KEY,
    ACT_DT_KEY,
    DAILY_ACTED_SLICE_KEY,
)
from billing.log_tariffication.py.jobs.core_acts import act_rows

from billing.library.python.yql_utils import yql
from billing.library.python.yt_utils.test_utils.utils import (
    create_subdirectory,
)
from billing.library.python.logfeller_utils.tests.utils import (
    mk_interval,
)
from billing.log_tariffication.py.tests.constants import (
    PREV_RUN_ID,
    CURR_RUN_ID,
    PREV_LOG_INTERVAL,
    CURR_LOG_INTERVAL,
)
from billing.log_tariffication.py.tests.integration.core_acts.common import (
    create_interim_rows_table,
    create_daily_table,
    create_headers_table,
    get_result_meta,
    TODAY,
    PAST,
    VERY_PAST,
    FUTURE,
    VERY_FUTURE,
)

TAX_POLICY_PCT_1 = 1
TAX_POLICY_PCT_2 = 2


@pytest.fixture(name='rows_dir')
def rows_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'rows')


@pytest.fixture(name='unprocessed_dir')
def unprocessed_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'unprocessed')


@pytest.fixture(name='daily_rows_dir')
def daily_rows_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'daily_rows')


@pytest.fixture(name='res_rows_dir')
def res_rows_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'res_rows')


@pytest.fixture(name='res_unprocessed_dir')
def res_unprocessed_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'res_unprocessed')


@pytest.fixture(name='res_headers_dir')
def res_headers_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'res_headers')


@pytest.fixture(name='run_job')
def run_job_fixture(
    yql_client,
    rows_dir,
    unprocessed_dir,
    daily_rows_dir,
    res_rows_dir,
    res_unprocessed_dir,
    res_headers_dir,
):

    def _wrapped(yt_client, transaction, meta, *args, **kwargs):
        return act_rows.run_job(
            yt_client,
            yql_client,
            transaction.transaction_id,
            meta,
            rows_dir,
            unprocessed_dir,
            daily_rows_dir,
            res_rows_dir,
            res_unprocessed_dir,
            res_headers_dir,
            *args,
            **kwargs
        )

    return _wrapped


@contextlib.contextmanager
def patch_run_query(yql_client, res_act_id):
    path = 'billing.log_tariffication.py.jobs.core_acts.act_rows.utils.yql.run_query'
    query = f'SELECT {res_act_id} INTO RESULT `act_sequence_pos`;'
    request = yql.run_query(yql_client, query)
    with mock.patch(path, return_value=request) as mock_obj:
        yield mock_obj


@contextlib.contextmanager
def patch_set_meta():
    path = 'billing.log_tariffication.py.jobs.core_acts.act_rows.utils.meta.set_log_tariff_meta'
    with mock.patch(path) as mock_obj:
        yield mock_obj


def get_meta(log_interval, run_id=None, act_dt=None, act_seq_pos=None, daily_slice=None):
    meta = {
        LOG_INTERVAL_KEY: log_interval.to_meta(),
        ACT_SEQUENCE_POS_KEY: act_seq_pos,
    }
    if run_id is not None:
        meta[RUN_ID_KEY] = run_id
    if act_dt is not None:
        meta[ACT_DT_KEY] = act_dt.strftime('%Y-%m-%d')
    if daily_slice is not None:
        meta[DAILY_ACTED_SLICE_KEY] = daily_slice.to_meta()
    return meta


class Case:
    _invoice_id_counter = [1]
    _invoice_ids_map = {}

    def __init__(self, uid_prefix=''):
        self._uid_prefix = uid_prefix
        self._row_id = 1
        self._service_order_id = 1

        self.rows = []
        self.unprocessed = []
        self.daily = []

    def _invoice_id(self, idx):
        case_invoice_ids_map = self._invoice_ids_map.setdefault(self._uid_prefix, {})
        res = case_invoice_ids_map.get(idx)
        if res is None:
            res, = self._invoice_id_counter
            self._invoice_id_counter[0] += 1
            case_invoice_ids_map[idx] = res
        return res

    def _fmt_rows(self, act_sum, dt=TODAY, invoice_id=1, tpp_id=TAX_POLICY_PCT_1):
        idx = '{}_{:05}'.format(self._uid_prefix, self._row_id)
        self._row_id += 1
        return (
            idx,
            dt,
            self._invoice_id(invoice_id),
            tpp_id,
            act_sum,
            self._service_order_id
        )

    def add_row(self, *args, is_daily=False, **kwargs):
        row = self._fmt_rows(*args, **kwargs)
        self.rows.append(row)
        if is_daily:
            self.daily.append(row[:1])
        return self

    def add_unprocessed(self, *args, is_daily=False, **kwargs):
        row = self._fmt_rows(*args, **kwargs)
        self.unprocessed.append(row)
        if is_daily:
            self.daily.append(row[:1])
        return self


def get_result(yt_client, res):
    uid_pattern = re.compile(r'(.*)_\d+')

    res_meta, rows_path, unprocessed_path, headers_path = res
    cases = {}
    act2case = {}
    for row in sorted(yt_client.read_table(rows_path), key=lambda r: r['UID']):
        case_name = uid_pattern.match(row['UID']).group(1)
        cases.setdefault(case_name, {}).setdefault('rows', []).append(row)
        act2case[row['act_id']] = case_name

    for row in sorted(yt_client.read_table(unprocessed_path), key=lambda r: r['UID']):
        case_name = uid_pattern.match(row['UID']).group(1)
        cases.setdefault(case_name, {}).setdefault('unprocessed', []).append(row)

    for row in sorted(yt_client.read_table(headers_path), key=lambda r: r['act_id']):
        case_name = act2case.get(row['act_id'], '_unknown_header')
        cases.setdefault(case_name, {}).setdefault('headers', []).append(row)

    meta = {
        'rows': get_result_meta(yt_client, rows_path),
        'unprocessed': get_result_meta(yt_client, unprocessed_path),
        'headers': get_result_meta(yt_client, headers_path),
    }

    res_meta = res_meta.copy()
    res_run_id = res_meta.pop('run_id')
    assert res_run_id

    return {'cases': cases, 'table_meta': meta, 'res_meta': res_meta}


def unpack_cases(cases):
    rows = []
    unprocessed = []
    daily = []

    for case in cases:
        rows.extend(case.rows)
        unprocessed.extend(case.unprocessed)
        daily.extend(case.daily)

    return rows, unprocessed, daily


def _gen_rounding_case():
    case = Case('rounding')
    for idx in range(100):
        case.add_row(0.01)
    for idx in range(99):
        case.add_unprocessed(-0.01)
    return case


CASES = [
    Case('grouping')
    .add_row(10.66, invoice_id=1, tpp_id=TAX_POLICY_PCT_1)
    .add_unprocessed(5.44, invoice_id=1, tpp_id=TAX_POLICY_PCT_1)
    .add_row(-13.42, invoice_id=1, tpp_id=TAX_POLICY_PCT_1)
    .add_row(666, invoice_id=1, tpp_id=TAX_POLICY_PCT_2)
    .add_unprocessed(13, invoice_id=2, tpp_id=TAX_POLICY_PCT_2),
    Case('dt_filter')
    .add_row(4, VERY_PAST)
    .add_row(5, PAST)
    .add_row(6, TODAY)
    .add_row(7, FUTURE)
    .add_row(8, VERY_FUTURE),
    Case('zero_sum')
    .add_row(6.66)
    .add_row(-3.21)
    .add_row(-3.45),
    Case('neg_sum')
    .add_row(6.66)
    .add_row(-3.22)
    .add_row(-3.45),
    _gen_rounding_case(),
    Case('dt_neg_sum')
    .add_row(-100, VERY_PAST)
    .add_row(66, PAST)
    .add_row(10, TODAY)
    .add_row(666, FUTURE),
    Case('daily')
    .add_row(10, TODAY)
    .add_row(20, TODAY, is_daily=True)
    .add_row(30, TODAY)
    .add_unprocessed(40, TODAY, is_daily=True)
    .add_unprocessed(50, TODAY),
]


def test_calculations(yt_client, run_job, rows_dir, daily_rows_dir, unprocessed_dir):
    rows, unprocessed, daily = unpack_cases(CASES)

    create_interim_rows_table(
        yt_client,
        ypath_join(rows_dir, PREV_RUN_ID),
        rows,
        get_meta(CURR_LOG_INTERVAL)
    )

    create_interim_rows_table(
        yt_client,
        ypath_join(unprocessed_dir, PREV_RUN_ID),
        unprocessed,
        get_meta(PREV_LOG_INTERVAL)
    )

    create_daily_table(
        yt_client,
        ypath_join(daily_rows_dir, PREV_RUN_ID),
        daily,
        get_meta(CURR_LOG_INTERVAL)
    )

    with yt_client.Transaction() as transaction:
        res = run_job(
            yt_client,
            transaction,
            # кол-во заказов должно переваливать за 10, чтобы проверить правильность выбора ACT_SEQUENCE_POS_KEY
            get_meta(CURR_LOG_INTERVAL, CURR_RUN_ID, TODAY, 3, CURR_LOG_INTERVAL.end),
        )

    return get_result(yt_client, res)


def test_rows_range(yt_client, yql_client, run_job, rows_dir, unprocessed_dir):
    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't00'), [], get_meta(mk_interval(0, 10)))
    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't10'), [], get_meta(mk_interval(10, 20)))
    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't20'), [], get_meta(mk_interval(20, 30)))
    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't30'), [], get_meta(mk_interval(30, 40)))
    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't40'), [], get_meta(mk_interval(40, 50)))

    create_interim_rows_table(yt_client, ypath_join(unprocessed_dir, 't00'), [], get_meta(mk_interval(0, 10)))

    with patch_run_query(yql_client, '123123213213') as mock_obj, patch_set_meta():
        with yt_client.Transaction() as transaction:
            run_job(
                yt_client,
                transaction,
                get_meta(mk_interval(10, 40), 't10', TODAY, 666, mk_interval(1, 2).end)
            )

    hamcrest.assert_that(
        mock_obj.call_args[0][2],
        hamcrest.has_entries({
            '$rows_dir': rows_dir,
            '$rows_first_table': 't10',
            '$rows_last_table': 't30',
        })
    )


@pytest.mark.parametrize(
    'daily_slice, res_first_table, res_last_table',
    [
        pytest.param(mk_interval(1, 2).end, '', '', id='old'),
        pytest.param(mk_interval(10, 20).end, '', '', id='prev'),
        pytest.param(mk_interval(30, 40).end, 't20', 't30', id='curr'),
    ]
)
def test_daily_range(yt_client, yql_client, run_job, rows_dir, daily_rows_dir,
                     unprocessed_dir, daily_slice, res_first_table, res_last_table):
    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't20'), [], get_meta(mk_interval(20, 50)))
    create_interim_rows_table(yt_client, ypath_join(unprocessed_dir, 't10'), [], get_meta(mk_interval(10, 20)))

    create_daily_table(yt_client, ypath_join(daily_rows_dir, 't00'), [], get_meta(mk_interval(0, 10)))
    create_daily_table(yt_client, ypath_join(daily_rows_dir, 't10'), [], get_meta(mk_interval(10, 20)))
    create_daily_table(yt_client, ypath_join(daily_rows_dir, 't20'), [], get_meta(mk_interval(20, 30)))
    create_daily_table(yt_client, ypath_join(daily_rows_dir, 't30'), [], get_meta(mk_interval(30, 40)))
    create_daily_table(yt_client, ypath_join(daily_rows_dir, 't40'), [], get_meta(mk_interval(40, 50)))

    with patch_run_query(yql_client, '42') as mock_obj, patch_set_meta():
        with yt_client.Transaction() as transaction:
            run_job(
                yt_client,
                transaction,
                get_meta(mk_interval(20, 50), 't50', TODAY, 666, daily_slice)
            )

    hamcrest.assert_that(
        mock_obj.call_args[0][2],
        hamcrest.has_entries({
            '$rows_dir': rows_dir,
            '$rows_first_table': 't20',
            '$rows_last_table': 't20',
            '$daily_rows_dir': daily_rows_dir,
            '$daily_rows_first_table': res_first_table,
            '$daily_rows_last_table': res_last_table,
        })
    )


def test_daily_rows_interval_split(yt_client, yql_client, run_job, rows_dir,
                                   daily_rows_dir, unprocessed_dir):
    create_interim_rows_table(yt_client, ypath_join(unprocessed_dir, 't00'), [], get_meta(mk_interval(0, 20)))

    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't00'), [], get_meta(mk_interval(20, 50)))

    create_daily_table(yt_client, ypath_join(daily_rows_dir, 't00'), [], get_meta(mk_interval(0, 10)))
    create_daily_table(yt_client, ypath_join(daily_rows_dir, 't10'), [], get_meta(mk_interval(10, 30)))
    create_daily_table(yt_client, ypath_join(daily_rows_dir, 't20'), [], get_meta(mk_interval(30, 40)))

    with patch_run_query(yql_client, '7') as mock_obj, patch_set_meta():
        with yt_client.Transaction() as transaction:
            run_job(
                yt_client,
                transaction,
                get_meta(
                    mk_interval(20, 50),
                    't30',
                    TODAY,
                    666,
                    mk_interval(0, 40).end,
                )
            )

    hamcrest.assert_that(
        mock_obj.call_args[0][2],
        hamcrest.has_entries({
            '$rows_dir': rows_dir,
            '$rows_first_table': 't00',
            '$rows_last_table': 't00',
            '$daily_rows_dir': daily_rows_dir,
            '$daily_rows_first_table': 't10',
            '$daily_rows_last_table': 't20',
        })
    )


def test_unprocessed_rows_check(yt_client, yql_client, run_job, rows_dir, unprocessed_dir):
    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't00'), [], get_meta(mk_interval(10, 20)))
    create_interim_rows_table(yt_client, ypath_join(unprocessed_dir, 't00'), [], get_meta(mk_interval(0, 11)))

    with pytest.raises(AssertionError) as exc_info:
        with patch_run_query(yql_client, '123123213213') as mock_obj, patch_set_meta():
            with yt_client.Transaction() as transaction:
                run_job(
                    yt_client,
                    transaction,
                    get_meta(mk_interval(10, 20), 't00', TODAY, 666, mk_interval(1, 2).end)
                )

    assert not mock_obj.called
    assert 'interval mismatch for {}'.format(unprocessed_dir) in exc_info.value.args[0]


def test_already_processed(yt_client, run_job, rows_dir, unprocessed_dir, res_rows_dir, res_unprocessed_dir,
                           res_headers_dir):

    meta = get_meta(mk_interval(10, 20), 't00', TODAY, 666, mk_interval(1, 2).end)

    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't00'), [], get_meta(mk_interval(10, 20)))
    create_interim_rows_table(yt_client, ypath_join(unprocessed_dir, 't00'), [], get_meta(mk_interval(0, 10)))

    create_interim_rows_table(yt_client, ypath_join(res_rows_dir, 't00'), [], meta)
    create_interim_rows_table(yt_client, ypath_join(res_unprocessed_dir, 't00'), [], meta)
    create_headers_table(
        yt_client,
        ypath_join(res_headers_dir, 't00'),
        [
            ('YB-123456',),
            ('YB-666666',),
            ('YB-000001',),
            ('YB-1',),
            ('YB-9',),
        ],
        meta
    )

    with yt_client.Transaction() as transaction:
        res_meta, res_rows_path, res_unprocessed_path, res_headers_path = run_job(yt_client, transaction, meta)

    assert res_meta == {**meta, ACT_SEQUENCE_POS_KEY: 666666}
    assert res_rows_path == ypath_join(res_rows_dir, 't00')
    assert res_unprocessed_path == ypath_join(res_unprocessed_dir, 't00')
    assert res_headers_path == ypath_join(res_headers_dir, 't00')


def test_already_processed_invalid_act_id(yt_client, run_job, rows_dir, unprocessed_dir,
                                          res_rows_dir, res_unprocessed_dir, res_headers_dir):
    meta = get_meta(mk_interval(10, 20), 't00', TODAY, 666, mk_interval(1, 2).end)

    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't00'), [], get_meta(mk_interval(10, 20)))
    create_interim_rows_table(yt_client, ypath_join(unprocessed_dir, 't00'), [], get_meta(mk_interval(0, 10)))

    create_interim_rows_table(yt_client, ypath_join(res_rows_dir, 't00'), [], meta)
    create_interim_rows_table(yt_client, ypath_join(res_unprocessed_dir, 't00'), [], meta)
    create_headers_table(
        yt_client,
        ypath_join(res_headers_dir, 't00'),
        [
            ('YB-q',),
            ('0',),
            ('B-000001',),
            ('YB-',),
        ],
        meta
    )

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            run_job(yt_client, transaction, meta)

    assert 'Invalid act_id pos: None' in exc_info.value.args[0]


def test_already_processed_partial(yt_client, yql_client, run_job, rows_dir,
                                   unprocessed_dir, res_rows_dir, res_headers_dir):
    meta = get_meta(mk_interval(10, 20), 't00', TODAY, 666, mk_interval(1, 2).end)

    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't00'), [], get_meta(mk_interval(10, 20)))
    create_interim_rows_table(yt_client, ypath_join(unprocessed_dir, 't00'), [], get_meta(mk_interval(0, 10)))

    create_interim_rows_table(yt_client, ypath_join(res_rows_dir, 't00'), [], meta)
    create_headers_table(yt_client, ypath_join(res_headers_dir, 't00'), [], meta)

    with pytest.raises(AssertionError) as exc_info:
        with patch_run_query(yql_client, '123123213213') as mock_obj:
            with yt_client.Transaction() as transaction:
                run_job(yt_client, transaction, meta)

    assert not mock_obj.called
    assert 'Partially formed result!' in exc_info.value.args[0]


def test_result_wrong_meta(yt_client, yql_client, run_job, rows_dir,
                           unprocessed_dir, res_rows_dir, res_unprocessed_dir,
                           res_headers_dir):
    meta = get_meta(mk_interval(10, 20), 't00', TODAY, 666, mk_interval(1, 2).end)

    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't00'), [], get_meta(mk_interval(10, 20)))
    create_interim_rows_table(yt_client, ypath_join(unprocessed_dir, 't00'), [], get_meta(mk_interval(0, 10)))

    create_interim_rows_table(yt_client, ypath_join(res_rows_dir, 't00'), [], meta)
    create_interim_rows_table(yt_client, ypath_join(res_unprocessed_dir, 't00'), [], meta)
    create_headers_table(
        yt_client,
        ypath_join(res_headers_dir, 't00'),
        [],
        {**meta, ACT_SEQUENCE_POS_KEY: 6666}
    )

    with pytest.raises(AssertionError) as exc_info:
        with patch_run_query(yql_client, '123123213213') as mock_obj:
            with yt_client.Transaction() as transaction:
                run_job(yt_client, transaction, meta)

    assert not mock_obj.called
    assert 'Bad meta in current table for {}'.format(res_headers_dir) in exc_info.value.args[0]


def test_no_rows(yt_client, run_job, rows_dir, unprocessed_dir):
    create_interim_rows_table(
        yt_client,
        ypath_join(rows_dir, PREV_RUN_ID),
        [],
        get_meta(CURR_LOG_INTERVAL)
    )

    create_interim_rows_table(
        yt_client,
        ypath_join(unprocessed_dir, PREV_RUN_ID),
        [],
        get_meta(PREV_LOG_INTERVAL)
    )

    with yt_client.Transaction() as transaction:
        res = run_job(
            yt_client,
            transaction,
            get_meta(CURR_LOG_INTERVAL, CURR_RUN_ID, TODAY, 666, mk_interval(1, 2).end)
        )

    return get_result(yt_client, res)


def test_wrong_act_seq_pos(yt_client, run_job, rows_dir, unprocessed_dir):
    create_interim_rows_table(
        yt_client,
        ypath_join(rows_dir, PREV_RUN_ID),
        [],
        get_meta(CURR_LOG_INTERVAL)
    )

    create_interim_rows_table(
        yt_client,
        ypath_join(unprocessed_dir, PREV_RUN_ID),
        [],
        get_meta(PREV_LOG_INTERVAL)
    )

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            run_job(
                yt_client,
                transaction,
                get_meta(CURR_LOG_INTERVAL, CURR_RUN_ID, TODAY, -1, mk_interval(1, 2).end)
            )

    assert 'Invalid act_id pos: -1' in exc_info.value.args[0]
