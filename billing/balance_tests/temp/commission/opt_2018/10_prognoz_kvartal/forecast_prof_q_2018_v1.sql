 CREATE OR REPLACE FORCE EDITIONABLE VIEW "BO"."V_OPT_2015_PROF_Q" ("CONTRACT_EID", "CONTRACT_ID", "AGENCY_ID", "CONTRACT_FROM_DT", "CONTRACT_TILL_DT", "DISCOUNT_TYPE", "FROM_DT", "TILL_DT", "TILL_DT_FC", "AMT_W_NDS_RUB", "AMT", "AMT_FOR_FORECAST", "AMT_AG", "AMT_AG_Q", "AMT_AG_PREV", "AMT_AG_PREV_FM", "FAILED", "EXCLUDED") AS 
  
 --!!!!!!!� ������� ���� ���������� ������ ����(160+ ������)!!!!!!!!
  with
  
   
  counting_date as (select date'2018-06-03' as dt from dual)
  --select * from counting_date;
  ,
  
  
  s_cltrl as (
      select d.*, 
             start_dt                                                 as from_dt,
             nvl(
                lead(start_dt) over(partition by code, contract_id
                                     order by collateral_id),
                add_months(trunc((select dt from counting_date), 'MM'), 3)
           ) - 1/24/60/60                                       as till_dt
      from (
        select distinct contract_id, collateral_id, start_dt , code, stamp
          from xxxx_contract_signed_attr
           ) d
)
--select  *from s_cltrl;  

,

s_base as (   
select distinct
           c.contract_eid      as contract_eid,
 		-- BALANCE-21757, BALANCE-21758
       -- BALANCE-22838
       -- ������������, ����, ������: ��� ��������� � ����� 1
       -- ���������� ��������� �������� �� ��.
       -- ����� �������� "�����" �� ������ ��������� (����� 1),
       -- ����� �������� � �� ����� ��������� (����� ������ ������).
       -- ��� �������� ����� ���������� external_id, �� ������ id.
       -- ����� �� ������� �������� ��������, ������� �������� �� ������ id,
       -- ��������� id ��� ������ ���������, ��������� id ����� ���������.
       case
        when c.contract_commission_type in (4, 10, 12, 13)
         and c.firm_id  = 1
        then
        nvl(
        (select distinct l.value_num
                from xxxx_new_comm_contract_basic l
               where l.contract_id = c.contract_id
                 and l.code = 'LINK_CONTRACT_ID'
--                 )
                 and l.value_num is not null),
           c.contract_id)
        else c.contract_id
       end                                            as contract_id,
		   c.contract_from_dt                         as contract_from_dt,
           c.contract_till_dt                         as contract_till_dt,
           c.invoice_eid                              as invoice_eid,
           c.invoice_id                               as invoice_id,
           c.invoice_dt                               as invoice_dt,
           i.invoice_type                             as invoice_type,
		   i.total_sum                                as total_sum,           
           c.currency                                 as currency,
           c.nds                                      as nds, 
           c.nds_pct                                  as nds_pct,
           c.loyal_client                             as loyal_clients,
             -- BALANCE-17175
           case nvl(c.commission_type, c.discount_type)
            when 22 then 1
            when 29 then 1  -- ������������ ����� ����������� ��� �������
            else nvl(c.commission_type, c.discount_type)
           end                                      as discount_type,
		       nvl(c.commission_type, c.discount_type)  as discount_type_src,
           c.payment_type                             as payment_type, 
            case
           -- ������ ��� �������/������� � �������
           when nvl(c.commission_type, c.discount_type) in (7, 37) then
          nvl((
            -- ������ �������� �� ���� "�� ������", �� ���� ������� �� ��
            -- �� �� ������� � ���.�������� �� ������� subclient_id
            -- subclient_id is     null: ��������� ��������� �����
            -- subclient_id is not null: ���������� ��������� �����
            select decode(p.value_num, null, 1, 0)
              from xxxx_invoice_repayment   ir
              join xxxx_extprops            p on p.object_id = ir.invoice_id
                                             and p.classname = 'PersonalAccount'
                                             and p.attrname = 'subclient_id'
             where ir.repayment_invoice_id = c.invoice_id 
               union
                select 1
                   from xxxx_invoice_repayment   ir              
                      where ir.repayment_invoice_id = c.invoice_id 
                     and not exists (
                     select 1 from xxxx_extprops p 
                      where p.object_id = ir.invoice_id
                     and p.classname = 'PersonalAccount'
                     and p.attrname = 'subclient_id')
        -- ���� ������ �� �������, �� �� �� ������, ������� �� ��������� ��.�.
        ), 0)
        else 0
        end                                   as is_agency_credit_line,
           c.contract_commission_type                 as commission_type
  from xxxx_new_comm_contract_basic c
  left join XXXX_INVOICE i on c.invoice_id = i.inv_id
  where (
                                               (
                                                     -- ������ �������/�����
                                                    c.contract_commission_type in (1, 2, 8, 21) and
                                                    -- �� ��������� ��� � ��� 22
                                                    nvl(c.commission_type,
                                                        c.discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 17, 19, 22, 25, 28, 29, 36, 37)
                                                )
                                                 or
                                                (
                                                    -- ������
                                                    c.contract_commission_type in (12, 13) and
                                                    nvl(c.commission_type, c.discount_type) in
                                                    (11)
                                                )
                                                or
                                                (
                                                    -- ������, �����������
                                                    c.contract_commission_type in (14) and
                                                    nvl(c.commission_type, c.discount_type) in
                                                    (33)
                                                )
                                                or
                                                
                                                (
                                                    -- �� ������������ ������� ��
                                                    c.contract_commission_type in (10, 16) and 1=1
                                                )
                                                or
                                                (
                                                    -- �� ������� ���������� ���,
                                                    -- ����� ���� � 15/16 ����.
                                                    -- ����������� ����� �����
                                                    -- �� ������ v_opt_2015_acts
                                                    -- BALANCE-23716
                                                    c.contract_commission_type in (6) and
                                                    nvl(c.commission_type, c.discount_type) in
                                                    (0, 1, 2, 3, 7, 8, 12, 15, 36)
                                                )
                                               or
												(
                                                    -- ������� ��������� � �������-����.��
                                                    c.contract_commission_type in (17) and
                                                nvl(c.commission_type, c.discount_type) in (25)
                                                )
                                                or
                                                (
                                                    -- �����, ��� �����
                                                    nvl(c.commission_type, c.discount_type) in
                                                    (1, 2, 3, 7, 11, 12, 14, 17, 19, 28, 29)
                                                )
                                              )
)
--select * from s_base order by  invoice_id ;
,



s_brands as (
        select /*+ materialize */
               atr.key_num          as client_id,
               atr.start_dt         as collateral_dt,
               c.start_dt           as dt,
               nvl(c.finish_dt, date'2018-06-03')    as finish_dt,
               min(c.MAIN_CLIENT_ID)     as main_client_id
          from xxxx_contract_signed_attr    atr
          -- BALANCE-27379: ������� � ���.������ �������� ���������, �����
            -- �������� ����������� ������, � ���.�� ��� ��������
          join xxxx_ui_contract_apex        c on c.contract_id = atr.contract_id
                                              where atr.code = 'BRAND_CLIENTS'
         group by atr.key_num, atr.start_dt, c.start_dt, nvl(c.finish_dt, date'2018-06-03')
)

--select  *from s_brands;
,
s_ar_acts_src as (
select 
    b.contract_eid,
           b.contract_id,
           b.invoice_eid,
           b.invoice_id,
           b.invoice_dt,
           b.invoice_type,
           b.contract_from_dt,
           b.contract_till_dt,
           b.currency,
           b.nds,
           b.payment_type,
           b.commission_type,
		 -- BALANCE-26651,  BALANCE-17175
       nvl(
           case xxx.commission_type
            when 22 then 1
            when 29 then 1  -- ������������ == �������
            else xxx.commission_type
           end,
           b.discount_type)                             as discount_type,
       -- BALANCE-26651
           nvl(xxx.commission_type, b.discount_type_src)     as discount_type_src,
           b.is_agency_credit_line,
           case
           when nvl(xxx.is_loyal, 0) = 1 and b.discount_type = 7
            then 1
            else 0
           end                                               as is_loyal,
           xxx.client_id                                     as client_id,
           xxx.agency_id                                      as agency_id,
--           nvl(brand.main_client_id, xxx.client_id)          as brand_id,
           xxx.act_id                                        as act_id,
           xxx.act_eid                                       as act_eid,
           xxx.act_dt                                         as act_dt,
           trunc(xxx.act_dt, 'MM')                            as from_dt,
           add_months(trunc(xxx.act_dt, 'MM'), 1) - 1/84600   as till_dt,
           xxx.amount                                         as amt_w_nds,
           xxx.amount-xxx.amount_nds-xxx.amount_nsp           as amt,
           xxx.amount*cr.rate                                 as amt_w_nds_rub,
           (xxx.amount-xxx.amount_nds-xxx.amount_nsp)*cr.rate as amt_rub
      from s_base        b
      join xxxx_new_comm_contract_basic     xxx on b.invoice_id = xxx.invoice_id
                                              and xxx.hidden <4
                                              -- BALANCE-24627: ����� ������ ���� �� �����
                                              and xxx.act_dt >= date'2015-03-01'
                                           -- BALANCE-24798: ��������� ��
                                              and ( xxx.is_loyal = 0 and xxx.act_dt >= date'2017-03-01'
                                                or xxx.act_dt < date'2017-03-01')

                                             
                                             
      join xxxx_currency_rate              cr on cr.cc = b.currency
                                              and cr.rate_dt = trunc(xxx.act_dt)
--      left outer
--      join s_brands                               brand on brand.client_id = xxx.client_id
     where b.commission_type in (1, 2, 3, 4, 5, 6, 8, 10, 11, 12, 13, 16, 17, 19, 21)
   and (
          -- base, prof
          (
            b.commission_type in (1, 2, 8, 21)
        and (
              
                -- BALANCE-24516: ����� ������� ��������� ������ ����� ������
                -- ���.���� ��� ��������, ��� ������� �������
                xxx.act_dt >= date'2017-03-01' and b.contract_till_dt > date'2017-03-01' and b.discount_type in (1, 2, 3, 7, 12, 36, 37)
                -- BALANCE-22085
                -- � ����� �� ����� � ���������� ��������� 2016:
                --  * ����������� ������ (11)
                --  * �������� ���� (25)
                --2016 ���
	         -- BALANCE-24734: ��������� 36 ���, ����� ������ ����� ������
             or  xxx.act_dt >= date'2017-03-01' and b.contract_till_dt  <= date'2017-03-01' and b.discount_type in (1, 2, 3, 7, 12, 25, 36)
             or  xxx.act_dt >= date'2016-03-01' and xxx.act_dt < date'2017-03-01'   and b.discount_type in (1, 2, 3, 7, 12, 25)
             -- 2015 ���
             or  xxx.act_dt >= date'2016-03-01' and b.contract_till_dt  <= date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12)
             or  xxx.act_dt  < date'2016-03-01' and b.discount_type in (1, 2, 3, 7, 11, 12)
            )
          )
          -- ua
       or (
             b.commission_type = 6 
  and (
                   -- BALANCE-25339
                   xxx.act_dt >= date'2017-04-01'
               and b.discount_type in (0, 1, 7, 8, 12, 36)
                   -- BALANCE-22914
                or   xxx.act_dt >= date'2016-04-01' and xxx.act_dt < date'2017-04-01'
               and b.discount_type in (0, 1, 8, 7, 15)
                or  xxx.act_dt <  date'2016-04-01'
               and b.discount_type in (1, 2, 3, 7, 12)
                )
     )
          -- spec
       or (b.commission_type = 3 and b.discount_type in (17))
          -- auto
       or (b.commission_type = 4 and b.discount_type in (19))
          -- sprav
       or (b.commission_type = 5 and b.discount_type = 12)
          -- estate
       or (b.commission_type in (10, 16) and 1=1)
          -- audio
       or (b.commission_type = 11 and b.discount_type_src = 29)
          -- market
       or (b.commission_type in (12, 13) and b.discount_type_src = 11)
          -- media verticals
       or (b.commission_type = 17 and b.discount_type = 25)
          -- verticals ico
       or (b.commission_type = 19 and b.discount_type = 7)
       )
)
--select  *from s_ar_acts_src;
,

s_ar_acts as (
     select  s.*,
       -- BALANCE-27709: ��� ������ ����� �� ����� ������, � ������� �����������
       --   ���, � �� �� "trunc(sysdate, 'MM') - 1", ����� �������
       --   ������� ������������ ��� ������ ����� ��������� ��
       --   ���� ������� �������
       nvl(b.main_client_id, s.client_id)               as brand_id   
       from s_ar_acts_src s
       left outer 
       join s_brands     b on b.client_id = s.client_id
            -- BALANCE-27379: ������� � ���.������ �������� ���������, �����
            -- �������� ����������� ������, � ���.�� ��� ��������
            and b.collateral_dt <= s.till_dt
        -- BALANCE-27363: ������ ����� ���.������ �� ����� ���� ������
          and s.till_dt between b.dt and b.finish_dt
)
--select  *from s_ar_acts;
,


s_acts_temp as (
    select b.*,
           -- ���������, ��� �� �������� ������ ���� -- 1 ������ � 1 ���
           count(distinct b.nds)      over (partition by b.contract_id) as nds_count,
           count(distinct b.currency) over (partition by b.contract_id) as currency_count
      from s_ar_acts  b
        -- BALANCE-24627: ������ ���� �� ���.���
     where b.act_dt >= date'2017-03-01'
)

--select * from s_acts_temp;
,


s_acts_src as ( select * from s_acts_temp
 where      -- BALANCE-22203
            -- BALANCE-22331
            -- BALANCE-23542
            -- BALANCE-22578
            -- � ����� �������� ��������� ������ ���� ������
            -- BALANCE-24627: ����������� (from_dt -> act_dt)
                act_dt >= date'2016-03-01'
            and currency_count = 1
            and nds_count = 1
            -- BALANCE-27062: belarus added
        and currency in ('RUR', 'BYN', 'BYR')
)

--select  *from s_acts_src;
,

--select * from s_acts_src;

s_quarters AS (
    select d.dt from_dt, add_months(d.dt, 3) - 1/24/60/60 as till_dt
      from (
         select add_months(date'2015-03-01', 3*(level-1)) as dt
           from dual
        connect by level <= 20
           ) d
)

--select * from s_quarters;
-- BALANCE-25122: ����������� �� ������� ��-��, ��� ����������� �������
,

s_excluded_agencies_pre as (
    select contract_id,
           key_num                                              as agency_id,
           start_dt                                            as from_dt
      from Xxxx_Contract_Signed_Attr
     where code = 'EXCLUDE_CLIENTS'
        -- ���� ������ �������� �� � �� ������� ���������� ��� �������� ������
        -- (����� ���� �� ����� ������ ������� �������, �� ��� ���� ������
        -- ��������, ����� ������ ������ ��������� ��� ������� � ����.��������)
       and start_dt < trunc((select dt from counting_date), 'MM')
        -- ������ ���������� ��-�� (���� ��� 0/null, �� �� ������ ���������)
       and value_num = 1
)

--select  * from s_excluded_agencies_pre;
-- BALANCE-26611: �������� �������� ������ �� ����� ��������
, 


s_excluded_agencies as (
    select d.*
      from s_excluded_agencies_pre      d
      join (
            select contract_id, max(from_dt) as from_dt
              from s_excluded_agencies_pre
             group by contract_id
           )                            l on l.contract_id = d.contract_id
                                         and l.from_dt = d.from_dt
)

--select * from s_excluded_agencies;
-- ������� ��-���������� �� ������� ��������
-- 2016 ����
, 

s_curr_contracts as (
    select d.contract_eid,        d.contract_id,        d.agency_id,
           d.contract_from_dt,    d.contract_till_dt,  
 		   decode(d.discount_type,
                -- ���������� ������� ��� ������, �.�. ����� �� ������ �������
                12, 7,
                -- ��� ������� ��� ������� 1 �����
                2, 1,
                3, 1,
                36, 1,
                37, 1,
                d.discount_type)                            as discount_type,
           q.from_dt, q.till_dt,
           d.from_dt                                        as month,
           sum(d.amt_w_nds_rub)                             as amt_w_nds_rub,
           sum(d.amt_rub)                                   as amt
      from s_acts_src     d
      join s_quarters               q on d.from_dt between q.from_dt and q.till_dt
     where d.commission_type in (2)
       and d.discount_type in (1, 2, 3, 7, 12, 36, 37)
        -- BALANCE-23037
       and d.contract_till_dt >  add_months(trunc((select dt from counting_date), 'YYYY'), 2)
       and not exists (
                select 1
                  from s_excluded_agencies  e
                 where e.contract_id = d.contract_id
                   and e.agency_id = d.agency_id
           )
       and d.is_loyal = 0
     group by d.contract_eid,       d.contract_id,      d.agency_id,
           d.contract_from_dt,      d.contract_till_dt,
           decode(d.discount_type,
                -- ���������� ������� ��� ������, �.�. ����� �� ������ �������
                12, 7,
                -- ��� ������� ��� ������� 1 �����
                2, 1,
                3, 1,
                36, 1,
                37, 1,
                d.discount_type),
           q.from_dt, q.till_dt,
           d.from_dt
)

--select * from s_curr_contracts;

-- ��� ������� �� ��-���, �� ������� ���� ������� � 2016 ����
, 

s_agency_stats_curr as (
  select d.*,
   -- ����������� ���� �� ��-�� � ������ ��������
              sum(amt) over (partition by agency_id, from_dt, discount_type
                                  order by month)                      as amt_m,
              -- ���� �� �������
             sum(amt) over (partition by agency_id, from_dt, discount_type)          as amt_q
   from ( select a.agency_id                                              as agency_id,
          decode(nvl(a.commission_type, a.discount_type),
                -- ���������� ������� ��� ������, �.�. ����� �� ������ �������
                12, 7,
                -- ��� ������� ��� ������� 1 �����
                2, 1,
                3, 1,
                36, 1,
                37, 1,
                nvl(a.commission_type, a.discount_type))            as discount_type,
 
           q.from_dt,
           trunc(a.act_dt, 'MM')                                        as month,
           sum((a.amount - a.amount_nds - a.amount_nsp)*cr.rate)              as amt
      from xxxx_new_comm_contract_basic         a
        
      join s_quarters       q on a.act_dt between q.from_dt and q.till_dt
      join xxxx_currency_rate              cr on cr.cc = a.currency
                                              and cr.rate_dt = trunc(a.act_dt)
     where a.hidden < 4
       and nvl(a.commission_type, a.discount_type) in    (1, 2, 3, 7, 12, 36, 37)
       and a.agency_id in (select agency_id from s_curr_contracts)
      and a.is_loyal = 0
     group by a.agency_id, q.from_dt, trunc(a.act_dt, 'MM'),
    decode(nvl(a.commission_type, a.discount_type),
                -- ���������� ������� ��� ������, �.�. ����� �� ������ �������
                12, 7,
                -- ��� ������� ��� ������� 1 �����
                2, 1,
                3, 1,
                36, 1,
                37, 1,
                nvl(a.commission_type, a.discount_type))
     )d
)


--select  * from s_agency_stats_curr  ;
,

-- ������������ ������� �� ��-���, �� ������� ���� ������� � 2016 ����
s_agency_stats_prev as (
    SELECT a.agency_id                                            as agency_id,
 decode(nvl(a.commission_type, a.discount_type),
                -- ���������� ������� ��� ������, �.�. ����� �� ������ �������
                12, 7,
                -- ��� ������� ��� ������� 1 �����
                2, 1,
                3, 1,
                36, 1,
                37, 1,
                nvl(a.commission_type, a.discount_type))          as discount_type,
           add_months(q.from_dt, 12)                              as from_dt,
           count(distinct trunc(a.act_dt, 'MM'))                  as month_cnt,
           sum(decode(q.from_dt,
                trunc(a.act_dt, 'MM'), (a.amount - a.amount_nds - a.amount_nsp)*cr.rate,
                0))                                               as amt_fm,
           sum((a.amount - a.amount_nds - a.amount_nsp)*cr.rate)            as amt
      from xxxx_new_comm_contract_basic         a
       join xxxx_currency_rate              cr on cr.cc = a.currency
                                              and cr.rate_dt = trunc(a.act_dt)
      join s_quarters       q on a.act_dt between q.from_dt and q.till_dt
     where a.hidden < 4
       and nvl(a.commission_type, a.discount_type) in (1, 2, 3, 7, 12, 36, 37)
       and a.agency_id in (select agency_id from s_curr_contracts)
       and a.is_loyal = 0
     group by a.agency_id, add_months(q.from_dt, 12),
           decode(nvl(a.commission_type, a.discount_type),
                -- ���������� ������� ��� ������, �.�. ����� �� ������ �������
                12, 7,
                -- ��� ������� ��� ������� 1 �����
                2, 1,
                3, 1,
                36, 1,
                37, 1,
                nvl(a.commission_type, a.discount_type))
)
--select * from s_agency_stats_prev;



select 
 d.contract_eid,        d.contract_id,        d.agency_id,
       d.contract_from_dt,    d.contract_till_dt,d.discount_type,
       d.from_dt, d.till_dt,
       d.month                  as till_dt_fc,
-- �� �����, �������� �� ����� �������
       d.amt_w_nds_rub          as amt_w_nds_rub,
       d.amt                    as amt,
       d.amt                    as amt_for_forecast,
       -- �� ����� (����������� ����)
       cs.amt_m                 as amt_ag,
-- �� �������
       cs.amt_q                 as amt_ag_q,
       nvl(ps.amt,0)                   as amt_ag_prev,
       nvl(ps.amt_fm,0)                as amt_ag_prev_fm,
       case
       -- BALANCE-28048: ��� ���������� �� ���� �������
        when nvl(ps.amt, 0) = 0 and d.discount_type = 1 then 0
        when nvl(ps.amt_fm, 0) = 0 then 1
        when ps.month_cnt != 3 then 1
        else 0
       end                      as failed,
       0                        as excluded

  from s_curr_contracts      d
  join s_agency_stats_curr   cs on cs.agency_id = d.agency_id
                               and cs.from_dt = d.from_dt
                               and cs.month = d.month
                               and cs.discount_type = d.discount_type
  left outer
  join s_agency_stats_prev   ps on ps.agency_id = d.agency_id
                               and ps.from_dt = d.from_dt
                               and ps.discount_type = d.discount_type
;
