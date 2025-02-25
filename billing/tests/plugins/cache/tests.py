# -*- coding: utf-8 -*-

# Unit tests for cache framework
# Uses whatever cache backend is set in the config file.
from __future__ import unicode_literals

import os
import time

from datetime import datetime
from unittest import TestCase

try:    # Use the same idiom as in cache backends
    import cPickle as pickle
except ImportError:
    import pickle

import mongoengine as me

from butils.application.plugins.cache import (
    InvalidCacheBackendError, DEFAULT_CACHE_ALIAS)

# functions/classes for complex data type tests
def f():
    return 42


class C:
    def m(n):
        return 24


def expensive_calculation():
    expensive_calculation.num_runs += 1
    return datetime.now()


class Poll(me.Document):
    meta = {'db_alias': 'trust'}
    question = me.StringField(max_length=200)
    answer = me.StringField(max_length=200)
    pub_date = me.DateTimeField('date published',
                                default=expensive_calculation)


def custom_key_func(key, key_prefix, version):
    "A customized cache key function"
    return 'CUSTOM-' + '-'.join([key_prefix, str(version), key])


_caches_setting_base = {
    'default': {},
    'prefix': {'KEY_PREFIX': 'cacheprefix{}'.format(os.getpid())},
    'v2': {'VERSION': 2},
    'custom_key': {'KEY_FUNCTION': custom_key_func},
    'custom_key2': {'KEY_FUNCTION': custom_key_func},
    'cull': {'OPTIONS': {'MAX_ENTRIES': 30}},
    'zero_cull': {'OPTIONS': {'CULL_FREQUENCY': 0, 'MAX_ENTRIES': 30}},
}


def caches_setting_for_tests(base=None, **params):
    # `base` is used to pull in the memcached config from the original settings,
    # `params` are test specific overrides and `_caches_settings_base` is the
    # base config for the tests.
    # This results in the following search order:
    # params -> _caches_setting_base -> base
    base = base or {}
    setting = dict((k, base.copy()) for k in _caches_setting_base.keys())
    for key, cache_params in setting.items():
        cache_params.update(_caches_setting_base[key])
        cache_params.update(params)
    return setting


#########  tmp hack to set up dev env
from xml.etree import ElementTree as et
from butils.application.plugins import mongodb
from butils.application.plugins.cache import get_cache
from butils.application.plugins.cache.utils import import_string

class Application(object):
        pass

app = Application()
cache_cfg_path = '../../../common_config/dev/cache.cfg.xml'
mongo_cfg_path = '../../../common_config/dev/db-conn-mongodb.cfg.xml'
with open(mongo_cfg_path) as cfg:
    mongo_cfg = et.fromstring(cfg.read())
mongodb.create(app, mongo_cfg)

with open(cache_cfg_path) as cfg:
    cache_cfg = et.fromstring(cfg.read())
def _parse_params(node):
    if node.getchildren():
        return dict([(child.tag, _parse_params(child))
                     for child in node.getchildren()])
    else:
        return node.text

caches = {}
caches_settings = {}
default_backend_cls = None
for backend in cache_cfg.findall('Backend'):
    backend_cls = import_string(backend.attrib['BackendClass'])
    params = _parse_params(backend)
    caches_settings[backend.attrib['id']] = params
    caches[backend.attrib['id']] = get_cache(backend.attrib['BackendClass'],
                                             **params)
    if backend.attrib['id'] == DEFAULT_CACHE_ALIAS:
        default_backend = backend.attrib['BackendClass']

for key, val in _caches_setting_base.items():
    caches_settings[key] = caches_settings[DEFAULT_CACHE_ALIAS].copy()
    caches_settings[key].update(val)
    if key not in caches:
        caches[key] = get_cache(default_backend, **caches_settings[key])

cache = caches[DEFAULT_CACHE_ALIAS]
###


class BaseCacheTests(object):
    # A common set of tests to apply to all cache backends

    def setUp(self):
        pass

    def tearDown(self):
        cache.clear()

    def test_simple(self):
        # Simple cache set/get works
        cache.set("key", "value")
        self.assertEqual(cache.get("key"), "value")

    def test_add(self):
        # A key can be added to a cache
        cache.add("addkey1", "value")
        result = cache.add("addkey1", "newvalue")
        self.assertFalse(result)
        self.assertEqual(cache.get("addkey1"), "value")

    def test_prefix(self):
        # Test for same cache key conflicts between shared backend
        cache.set('somekey', 'value')

        # should not be set in the prefixed cache
        self.assertFalse(caches['prefix'].has_key('somekey'))

        caches['prefix'].set('somekey', 'value2')

        self.assertEqual(cache.get('somekey'), 'value')
        self.assertEqual(caches['prefix'].get('somekey'), 'value2')

    def test_non_existent(self):
        # Non-existent cache keys return as None/default
        # get with non-existent keys
        self.assertIsNone(cache.get("does_not_exist"))
        self.assertEqual(cache.get("does_not_exist", "bang!"), "bang!")

    def test_get_many(self):
        # Multiple cache keys can be returned using get_many
        cache.set('a', 'a')
        cache.set('b', 'b')
        cache.set('c', 'c')
        cache.set('d', 'd')
        self.assertDictEqual(cache.get_many(['a', 'c', 'd']),
                             {'a': 'a', 'c': 'c', 'd': 'd'})
        self.assertDictEqual(cache.get_many(['a', 'b', 'e']),
                             {'a': 'a', 'b': 'b'})

    def test_delete(self):
        # Cache keys can be deleted
        cache.set("key1", "spam")
        cache.set("key2", "eggs")
        self.assertEqual(cache.get("key1"), "spam")
        cache.delete("key1")
        self.assertIsNone(cache.get("key1"))
        self.assertEqual(cache.get("key2"), "eggs")

    def test_has_key(self):
        # The cache can be inspected for cache keys
        cache.set("hello1", "goodbye1")
        self.assertTrue(cache.has_key("hello1"))
        self.assertFalse(cache.has_key("goodbye1"))
        cache.set("no_expiry", "here", None)
        self.assertTrue(cache.has_key("no_expiry"))

    def test_in(self):
        # The in operator can be used to inspect cache contents
        cache.set("hello2", "goodbye2")
        self.assertIn("hello2", cache)
        self.assertNotIn("goodbye2", cache)

    def test_incr(self):
        # Cache values can be incremented
        cache.set('answer', 41)
        self.assertEqual(cache.incr('answer'), 42)
        self.assertEqual(cache.get('answer'), 42)
        self.assertEqual(cache.incr('answer', 10), 52)
        self.assertEqual(cache.get('answer'), 52)
        self.assertEqual(cache.incr('answer', -10), 42)
        self.assertRaises(ValueError, cache.incr, 'does_not_exist')

    def test_decr(self):
        # Cache values can be decremented
        cache.set('answer', 43)
        self.assertEqual(cache.decr('answer'), 42)
        self.assertEqual(cache.get('answer'), 42)
        self.assertEqual(cache.decr('answer', 10), 32)
        self.assertEqual(cache.get('answer'), 32)
        self.assertEqual(cache.decr('answer', -10), 42)
        self.assertRaises(ValueError, cache.decr, 'does_not_exist')

    def test_close(self):
        self.assertTrue(hasattr(cache, 'close'))
        cache.close()

    def test_data_types(self):
        # Many different data types can be cached
        stuff = {
            'string': 'this is a string',
            'int': 42,
            'list': [1, 2, 3, 4],
            'tuple': (1, 2, 3, 4),
            'dict': {'A': 1, 'B': 2},
            'function': f,
            'class': C,
        }
        cache.set("stuff", stuff)
        self.assertEqual(cache.get("stuff"), stuff)

    def test_cache_read_for_model_instance(self):
        # Don't want fields with callable as default to be called on cache read
        expensive_calculation.num_runs = 0
        Poll.objects.all().delete()
        my_poll = Poll.objects.create(question="Well?")
        self.assertEqual(Poll.objects.count(), 1)
        pub_date = my_poll.pub_date
        cache.set('question', my_poll)
        cached_poll = cache.get('question')
        self.assertEqual(cached_poll.pub_date, pub_date)
        # We only want the default expensive calculation run once
        self.assertEqual(expensive_calculation.num_runs, 1)

    # def test_cache_write_for_model_instance_with_deferred(self):
    #     # Don't want fields with callable as default to be called on cache write
    #     expensive_calculation.num_runs = 0
    #     Poll.objects.all().delete()
    #     Poll.objects.create(question="What?")
    #     self.assertEqual(expensive_calculation.num_runs, 1)
    #     defer_qs = Poll.objects.all().defer('question')
    #     self.assertEqual(defer_qs.count(), 1)
    #     self.assertEqual(expensive_calculation.num_runs, 1)
    #     cache.set('deferred_queryset', defer_qs)
    #     # cache set should not re-evaluate default functions
    #     self.assertEqual(expensive_calculation.num_runs, 1)

    # def test_cache_read_for_model_instance_with_deferred(self):
    #     # Don't want fields with callable as default to be called on cache read
    #     expensive_calculation.num_runs = 0
    #     Poll.objects.all().delete()
    #     Poll.objects.create(question="What?")
    #     self.assertEqual(expensive_calculation.num_runs, 1)
    #     defer_qs = Poll.objects.all().defer('question')
    #     self.assertEqual(defer_qs.count(), 1)
    #     cache.set('deferred_queryset', defer_qs)
    #     self.assertEqual(expensive_calculation.num_runs, 1)
    #     runs_before_cache_read = expensive_calculation.num_runs
    #     cache.get('deferred_queryset')
    #     # We only want the default expensive calculation run on creation and set
    #     self.assertEqual(expensive_calculation.num_runs, runs_before_cache_read)

    def test_expiration(self):
        # Cache values can be set to expire
        cache.set('expire1', 'very quickly', 1)
        cache.set('expire2', 'very quickly', 1)
        cache.set('expire3', 'very quickly', 1)

        time.sleep(2)
        self.assertIsNone(cache.get("expire1"))

        cache.add("expire2", "newvalue")
        self.assertEqual(cache.get("expire2"), "newvalue")
        self.assertFalse(cache.has_key("expire3"))

    def test_unicode(self):
        # Unicode values can be cached
        stuff = {
            'ascii': 'ascii_value',
            'unicode_ascii': 'Iñtërnâtiônàlizætiøn1',
            'Iñtërnâtiônàlizætiøn': 'Iñtërnâtiônàlizætiøn2',
            'ascii2': {'x': 1}
        }
        # Test `set`
        for (key, value) in stuff.items():
            cache.set(key, value)
            self.assertEqual(cache.get(key), value)

        # Test `add`
        for (key, value) in stuff.items():
            cache.delete(key)
            cache.add(key, value)
            self.assertEqual(cache.get(key), value)

        # Test `set_many`
        for (key, value) in stuff.items():
            cache.delete(key)
        cache.set_many(stuff)
        for (key, value) in stuff.items():
            self.assertEqual(cache.get(key), value)

    def test_binary_string(self):
        # Binary strings should be cacheable
        from zlib import compress, decompress
        value = 'value_to_be_compressed'
        compressed_value = compress(value.encode())

        # Test set
        cache.set('binary1', compressed_value)
        compressed_result = cache.get('binary1')
        self.assertEqual(compressed_value, compressed_result)
        self.assertEqual(value, decompress(compressed_result).decode())

        # Test add
        cache.add('binary1-add', compressed_value)
        compressed_result = cache.get('binary1-add')
        self.assertEqual(compressed_value, compressed_result)
        self.assertEqual(value, decompress(compressed_result).decode())

        # Test set_many
        cache.set_many({'binary1-set_many': compressed_value})
        compressed_result = cache.get('binary1-set_many')
        self.assertEqual(compressed_value, compressed_result)
        self.assertEqual(value, decompress(compressed_result).decode())

    def test_set_many(self):
        # Multiple keys can be set using set_many
        cache.set_many({"key1": "spam", "key2": "eggs"})
        self.assertEqual(cache.get("key1"), "spam")
        self.assertEqual(cache.get("key2"), "eggs")

    def test_set_many_expiration(self):
        # set_many takes a second ``timeout`` parameter
        cache.set_many({"key1": "spam", "key2": "eggs"}, 1)
        time.sleep(2)
        self.assertIsNone(cache.get("key1"))
        self.assertIsNone(cache.get("key2"))

    def test_delete_many(self):
        # Multiple keys can be deleted using delete_many
        cache.set("key1", "spam")
        cache.set("key2", "eggs")
        cache.set("key3", "ham")
        cache.delete_many(["key1", "key2"])
        self.assertIsNone(cache.get("key1"))
        self.assertIsNone(cache.get("key2"))
        self.assertEqual(cache.get("key3"), "ham")

    def test_clear(self):
        # The cache can be emptied using clear
        cache.set("key1", "spam")
        cache.set("key2", "eggs")
        cache.clear()
        self.assertIsNone(cache.get("key1"))
        self.assertIsNone(cache.get("key2"))

    def test_long_timeout(self):
        '''
        Using a timeout greater than 30 days makes memcached think
        it is an absolute expiration timestamp instead of a relative
        offset. Test that we honour this convention. Refs #12399.
        '''
        cache.set('key1', 'eggs', 60 * 60 * 24 * 30 + 1)  # 30 days + 1 second
        self.assertEqual(cache.get('key1'), 'eggs')

        cache.add('key2', 'ham', 60 * 60 * 24 * 30 + 1)
        self.assertEqual(cache.get('key2'), 'ham')

        cache.set_many({'key3': 'sausage', 'key4': 'lobster bisque'},
                       60 * 60 * 24 * 30 + 1)
        self.assertEqual(cache.get('key3'), 'sausage')
        self.assertEqual(cache.get('key4'), 'lobster bisque')

    def test_forever_timeout(self):
        '''
        Passing in None into timeout results in a value that is cached forever
        '''
        cache.set('key1', 'eggs', None)
        self.assertEqual(cache.get('key1'), 'eggs')

        cache.add('key2', 'ham', None)
        self.assertEqual(cache.get('key2'), 'ham')
        added = cache.add('key1', 'new eggs', None)
        self.assertEqual(added, False)
        self.assertEqual(cache.get('key1'), 'eggs')

        cache.set_many({'key3': 'sausage', 'key4': 'lobster bisque'}, None)
        self.assertEqual(cache.get('key3'), 'sausage')
        self.assertEqual(cache.get('key4'), 'lobster bisque')

    def test_zero_timeout(self):
        '''
        Passing in zero into timeout results in a value that is not cached
        '''
        cache.set('key1', 'eggs', 0)
        self.assertIsNone(cache.get('key1'))

        cache.add('key2', 'ham', 0)
        self.assertIsNone(cache.get('key2'))

        cache.set_many({'key3': 'sausage', 'key4': 'lobster bisque'}, 0)
        self.assertIsNone(cache.get('key3'))
        self.assertIsNone(cache.get('key4'))

    def test_float_timeout(self):
        # Make sure a timeout given as a float doesn't crash anything.
        cache.set("key1", "spam", 100.2)
        self.assertEqual(cache.get("key1"), "spam")

    def _perform_cull_test(self, cull_cache, initial_count, final_count):
        # Create initial cache key entries. This will overflow the cache,
        # causing a cull.
        for i in range(1, initial_count):
            cull_cache.set('cull%d' % i, 'value', 1000)
        count = 0
        # Count how many keys are left in the cache.
        for i in range(1, initial_count):
            if cull_cache.has_key('cull%d' % i):
                count = count + 1
        self.assertEqual(count, final_count)

    def test_cull(self):
        self._perform_cull_test(caches['cull'], 50, 29)

    def test_zero_cull(self):
        self._perform_cull_test(caches['zero_cull'], 50, 19)

    def test_cache_versioning_get_set(self):
        # set, using default version = 1
        cache.set('answer1', 42)
        self.assertEqual(cache.get('answer1'), 42)
        self.assertEqual(cache.get('answer1', version=1), 42)
        self.assertIsNone(cache.get('answer1', version=2))

        self.assertIsNone(caches['v2'].get('answer1'))
        self.assertEqual(caches['v2'].get('answer1', version=1), 42)
        self.assertIsNone(caches['v2'].get('answer1', version=2))

        # set, default version = 1, but manually override version = 2
        cache.set('answer2', 42, version=2)
        self.assertIsNone(cache.get('answer2'))
        self.assertIsNone(cache.get('answer2', version=1))
        self.assertEqual(cache.get('answer2', version=2), 42)

        self.assertEqual(caches['v2'].get('answer2'), 42)
        self.assertIsNone(caches['v2'].get('answer2', version=1))
        self.assertEqual(caches['v2'].get('answer2', version=2), 42)

        # v2 set, using default version = 2
        caches['v2'].set('answer3', 42)
        self.assertIsNone(cache.get('answer3'))
        self.assertIsNone(cache.get('answer3', version=1))
        self.assertEqual(cache.get('answer3', version=2), 42)

        self.assertEqual(caches['v2'].get('answer3'), 42)
        self.assertIsNone(caches['v2'].get('answer3', version=1))
        self.assertEqual(caches['v2'].get('answer3', version=2), 42)

        # v2 set, default version = 2, but manually override version = 1
        caches['v2'].set('answer4', 42, version=1)
        self.assertEqual(cache.get('answer4'), 42)
        self.assertEqual(cache.get('answer4', version=1), 42)
        self.assertIsNone(cache.get('answer4', version=2))

        self.assertIsNone(caches['v2'].get('answer4'))
        self.assertEqual(caches['v2'].get('answer4', version=1), 42)
        self.assertIsNone(caches['v2'].get('answer4', version=2))

    def test_cache_versioning_add(self):

        # add, default version = 1, but manually override version = 2
        cache.add('answer1', 42, version=2)
        self.assertIsNone(cache.get('answer1', version=1))
        self.assertEqual(cache.get('answer1', version=2), 42)

        cache.add('answer1', 37, version=2)
        self.assertIsNone(cache.get('answer1', version=1))
        self.assertEqual(cache.get('answer1', version=2), 42)

        cache.add('answer1', 37, version=1)
        self.assertEqual(cache.get('answer1', version=1), 37)
        self.assertEqual(cache.get('answer1', version=2), 42)

        # v2 add, using default version = 2
        caches['v2'].add('answer2', 42)
        self.assertIsNone(cache.get('answer2', version=1))
        self.assertEqual(cache.get('answer2', version=2), 42)

        caches['v2'].add('answer2', 37)
        self.assertIsNone(cache.get('answer2', version=1))
        self.assertEqual(cache.get('answer2', version=2), 42)

        caches['v2'].add('answer2', 37, version=1)
        self.assertEqual(cache.get('answer2', version=1), 37)
        self.assertEqual(cache.get('answer2', version=2), 42)

        # v2 add, default version = 2, but manually override version = 1
        caches['v2'].add('answer3', 42, version=1)
        self.assertEqual(cache.get('answer3', version=1), 42)
        self.assertIsNone(cache.get('answer3', version=2))

        caches['v2'].add('answer3', 37, version=1)
        self.assertEqual(cache.get('answer3', version=1), 42)
        self.assertIsNone(cache.get('answer3', version=2))

        caches['v2'].add('answer3', 37)
        self.assertEqual(cache.get('answer3', version=1), 42)
        self.assertEqual(cache.get('answer3', version=2), 37)

    def test_cache_versioning_has_key(self):
        cache.set('answer1', 42)

        # has_key
        self.assertTrue(cache.has_key('answer1'))
        self.assertTrue(cache.has_key('answer1', version=1))
        self.assertFalse(cache.has_key('answer1', version=2))

        self.assertFalse(caches['v2'].has_key('answer1'))
        self.assertTrue(caches['v2'].has_key('answer1', version=1))
        self.assertFalse(caches['v2'].has_key('answer1', version=2))

    def test_cache_versioning_delete(self):
        cache.set('answer1', 37, version=1)
        cache.set('answer1', 42, version=2)
        cache.delete('answer1')
        self.assertIsNone(cache.get('answer1', version=1))
        self.assertEqual(cache.get('answer1', version=2), 42)

        cache.set('answer2', 37, version=1)
        cache.set('answer2', 42, version=2)
        cache.delete('answer2', version=2)
        self.assertEqual(cache.get('answer2', version=1), 37)
        self.assertIsNone(cache.get('answer2', version=2))

        cache.set('answer3', 37, version=1)
        cache.set('answer3', 42, version=2)
        caches['v2'].delete('answer3')
        self.assertEqual(cache.get('answer3', version=1), 37)
        self.assertIsNone(cache.get('answer3', version=2))

        cache.set('answer4', 37, version=1)
        cache.set('answer4', 42, version=2)
        caches['v2'].delete('answer4', version=1)
        self.assertIsNone(cache.get('answer4', version=1))
        self.assertEqual(cache.get('answer4', version=2), 42)

    def test_cache_versioning_incr_decr(self):
        cache.set('answer1', 37, version=1)
        cache.set('answer1', 42, version=2)
        cache.incr('answer1')
        self.assertEqual(cache.get('answer1', version=1), 38)
        self.assertEqual(cache.get('answer1', version=2), 42)
        cache.decr('answer1')
        self.assertEqual(cache.get('answer1', version=1), 37)
        self.assertEqual(cache.get('answer1', version=2), 42)

        cache.set('answer2', 37, version=1)
        cache.set('answer2', 42, version=2)
        cache.incr('answer2', version=2)
        self.assertEqual(cache.get('answer2', version=1), 37)
        self.assertEqual(cache.get('answer2', version=2), 43)
        cache.decr('answer2', version=2)
        self.assertEqual(cache.get('answer2', version=1), 37)
        self.assertEqual(cache.get('answer2', version=2), 42)

        cache.set('answer3', 37, version=1)
        cache.set('answer3', 42, version=2)
        caches['v2'].incr('answer3')
        self.assertEqual(cache.get('answer3', version=1), 37)
        self.assertEqual(cache.get('answer3', version=2), 43)
        caches['v2'].decr('answer3')
        self.assertEqual(cache.get('answer3', version=1), 37)
        self.assertEqual(cache.get('answer3', version=2), 42)

        cache.set('answer4', 37, version=1)
        cache.set('answer4', 42, version=2)
        caches['v2'].incr('answer4', version=1)
        self.assertEqual(cache.get('answer4', version=1), 38)
        self.assertEqual(cache.get('answer4', version=2), 42)
        caches['v2'].decr('answer4', version=1)
        self.assertEqual(cache.get('answer4', version=1), 37)
        self.assertEqual(cache.get('answer4', version=2), 42)

    def test_cache_versioning_get_set_many(self):
        # set, using default version = 1
        cache.set_many({'ford1': 37, 'arthur1': 42})
        self.assertDictEqual(cache.get_many(['ford1', 'arthur1']),
                         {'ford1': 37, 'arthur1': 42})
        self.assertDictEqual(cache.get_many(['ford1', 'arthur1'], version=1),
                         {'ford1': 37, 'arthur1': 42})
        self.assertDictEqual(cache.get_many(['ford1', 'arthur1'],version=2),{})

        self.assertDictEqual(caches['v2'].get_many(['ford1', 'arthur1']), {})
        self.assertDictEqual(caches['v2'].get_many(['ford1', 'arthur1'], version=1),
                         {'ford1': 37, 'arthur1': 42})
        self.assertDictEqual(caches['v2'].get_many(['ford1', 'arthur1'], version=2), {})

        # set, default version = 1, but manually override version = 2
        cache.set_many({'ford2': 37, 'arthur2': 42}, version=2)
        self.assertDictEqual(cache.get_many(['ford2', 'arthur2']), {})
        self.assertDictEqual(cache.get_many(['ford2', 'arthur2'], version=1), {})
        self.assertDictEqual(cache.get_many(['ford2', 'arthur2'], version=2),
                         {'ford2': 37, 'arthur2': 42})

        self.assertDictEqual(caches['v2'].get_many(['ford2', 'arthur2']),
                         {'ford2': 37, 'arthur2': 42})
        self.assertDictEqual(caches['v2'].get_many(['ford2', 'arthur2'], version=1), {})
        self.assertDictEqual(caches['v2'].get_many(['ford2', 'arthur2'], version=2),
                         {'ford2': 37, 'arthur2': 42})

        # v2 set, using default version = 2
        caches['v2'].set_many({'ford3': 37, 'arthur3': 42})
        self.assertDictEqual(cache.get_many(['ford3', 'arthur3']), {})
        self.assertDictEqual(cache.get_many(['ford3', 'arthur3'], version=1), {})
        self.assertDictEqual(cache.get_many(['ford3', 'arthur3'], version=2),
                         {'ford3': 37, 'arthur3': 42})

        self.assertDictEqual(caches['v2'].get_many(['ford3', 'arthur3']),
                         {'ford3': 37, 'arthur3': 42})
        self.assertDictEqual(caches['v2'].get_many(['ford3', 'arthur3'], version=1), {})
        self.assertDictEqual(caches['v2'].get_many(['ford3', 'arthur3'], version=2),
                         {'ford3': 37, 'arthur3': 42})

        # v2 set, default version = 2, but manually override version = 1
        caches['v2'].set_many({'ford4': 37, 'arthur4': 42}, version=1)
        self.assertDictEqual(cache.get_many(['ford4', 'arthur4']),
                         {'ford4': 37, 'arthur4': 42})
        self.assertDictEqual(cache.get_many(['ford4', 'arthur4'], version=1),
                         {'ford4': 37, 'arthur4': 42})
        self.assertDictEqual(cache.get_many(['ford4', 'arthur4'], version=2), {})

        self.assertDictEqual(caches['v2'].get_many(['ford4', 'arthur4']), {})
        self.assertDictEqual(caches['v2'].get_many(['ford4', 'arthur4'], version=1),
                         {'ford4': 37, 'arthur4': 42})
        self.assertDictEqual(caches['v2'].get_many(['ford4', 'arthur4'], version=2), {})

    def test_incr_version(self):
        cache.set('answer', 42, version=2)
        self.assertIsNone(cache.get('answer'))
        self.assertIsNone(cache.get('answer', version=1))
        self.assertEqual(cache.get('answer', version=2), 42)
        self.assertIsNone(cache.get('answer', version=3))

        self.assertEqual(cache.incr_version('answer', version=2), 3)
        self.assertIsNone(cache.get('answer'))
        self.assertIsNone(cache.get('answer', version=1))
        self.assertIsNone(cache.get('answer', version=2))
        self.assertEqual(cache.get('answer', version=3), 42)

        caches['v2'].set('answer2', 42)
        self.assertEqual(caches['v2'].get('answer2'), 42)
        self.assertIsNone(caches['v2'].get('answer2', version=1))
        self.assertEqual(caches['v2'].get('answer2', version=2), 42)
        self.assertIsNone(caches['v2'].get('answer2', version=3))

        self.assertEqual(caches['v2'].incr_version('answer2'), 3)
        self.assertIsNone(caches['v2'].get('answer2'))
        self.assertIsNone(caches['v2'].get('answer2', version=1))
        self.assertIsNone(caches['v2'].get('answer2', version=2))
        self.assertEqual(caches['v2'].get('answer2', version=3), 42)

        self.assertRaises(ValueError, cache.incr_version, 'does_not_exist')

    def test_decr_version(self):
        cache.set('answer', 42, version=2)
        self.assertIsNone(cache.get('answer'))
        self.assertIsNone(cache.get('answer', version=1))
        self.assertEqual(cache.get('answer', version=2), 42)

        self.assertEqual(cache.decr_version('answer', version=2), 1)
        self.assertEqual(cache.get('answer'), 42)
        self.assertEqual(cache.get('answer', version=1), 42)
        self.assertIsNone(cache.get('answer', version=2))

        caches['v2'].set('answer2', 42)
        self.assertEqual(caches['v2'].get('answer2'), 42)
        self.assertIsNone(caches['v2'].get('answer2', version=1))
        self.assertEqual(caches['v2'].get('answer2', version=2), 42)

        self.assertEqual(caches['v2'].decr_version('answer2'), 1)
        self.assertIsNone(caches['v2'].get('answer2'))
        self.assertEqual(caches['v2'].get('answer2', version=1), 42)
        self.assertIsNone(caches['v2'].get('answer2', version=2))

        self.assertRaises(ValueError, cache.decr_version, 'does_not_exist', version=2)

    def test_custom_key_func(self):
        # Two caches with different key functions aren't visible to each other
        cache.set('answer1', 42)
        self.assertEqual(cache.get('answer1'), 42)
        self.assertIsNone(caches['custom_key'].get('answer1'))
        self.assertIsNone(caches['custom_key2'].get('answer1'))

        caches['custom_key'].set('answer2', 42)
        self.assertIsNone(cache.get('answer2'))
        self.assertEqual(caches['custom_key'].get('answer2'), 42)
        self.assertEqual(caches['custom_key2'].get('answer2'), 42)


# class LocMemCacheTests(BaseCacheTests, TestCase):

#     def setUp(self):
#         super(LocMemCacheTests, self).setUp()

#         # LocMem requires a hack to make the other caches
#         # share a data store with the 'normal' cache.
#         caches['prefix']._cache = cache._cache
#         caches['prefix']._expire_info = cache._expire_info

#         caches['v2']._cache = cache._cache
#         caches['v2']._expire_info = cache._expire_info

#         caches['custom_key']._cache = cache._cache
#         caches['custom_key']._expire_info = cache._expire_info

#         caches['custom_key2']._cache = cache._cache
#         caches['custom_key2']._expire_info = cache._expire_info

#     # @override_settings(CACHES={
#     #     'default': {'BACKEND': 'django.core.cache.backends.locmem.LocMemCache'},
#     #     'other': {
#     #         'BACKEND': 'django.core.cache.backends.locmem.LocMemCache',
#     #         'LOCATION': 'other'
#     #     },
#     # })
#     # def test_multiple_caches(self):
#     #     "Check that multiple locmem caches are isolated"
#     #     cache.set('value', 42)
#     #     self.assertEqual(caches['default'].get('value'), 42)
#     #     self.assertIsNone(caches['other'].get('value'))

#     def test_locking_on_pickle(self):
#         """#20613/#18541 -- Ensures pickling is done outside of the lock."""
#         bad_obj = PicklingSideEffect(cache)
#         cache.set('set', bad_obj)
#         self.assertFalse(bad_obj.locked, "Cache was locked during pickling")

#         cache.add('add', bad_obj)
#         self.assertFalse(bad_obj.locked, "Cache was locked during pickling")

#     def test_incr_decr_timeout(self):
#         """incr/decr does not modify expiry time (matches memcached behavior)"""
#         key = 'value'
#         _key = cache.make_key(key)
#         cache.set(key, 1, timeout=cache.default_timeout * 10)
#         expire = cache._expire_info[_key]
#         cache.incr(key)
#         self.assertEqual(expire, cache._expire_info[_key])
#         cache.decr(key)
#         self.assertEqual(expire, cache._expire_info[_key])


class MongoDBCacheTests(BaseCacheTests, TestCase):

    available_apps = ['cache']

    def setUp(self):
        # The super calls needs to happen first for the settings override.
        super(MongoDBCacheTests, self).setUp()

    def tearDown(self):
        # The super call needs to happen first because it uses the database.
        super(MongoDBCacheTests, self).tearDown()

    def test_zero_cull(self):
        self._perform_cull_test(caches['zero_cull'], 50, 18)


if __name__ == '__main__':
    import unittest
    unittest.main()
