import json
import threading
import unittest
from http.server import ThreadingHTTPServer
from urllib.error import HTTPError
from urllib.request import urlopen

from server import Handler


class ApiTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server = ThreadingHTTPServer(('127.0.0.1', 0), Handler)
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()
        cls.base_url = f'http://127.0.0.1:{cls.server.server_port}'

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()
        cls.thread.join()

    def get(self, path):
        try:
            response = urlopen(self.base_url + path)
        except HTTPError as error:
            response = error
        with response:
            return response.status, json.load(response)

    def test_catalog(self):
        status, platforms = self.get('/platforms')
        self.assertEqual(status, 200)
        self.assertEqual(len(platforms), 40)
        module_ids = set()
        for platform in platforms:
            status, modules = self.get(f'/platforms/{platform["id"]}/modules')
            self.assertEqual(status, 200)
            self.assertEqual(len(modules), 40)
            module_ids.update(module['id'] for module in modules)
        self.assertEqual(module_ids, set(range(1, 1601)))

    def test_pagination(self):
        ids = []
        for offset in range(0, 2000, 137):
            status, page = self.get(f'/modules/1/photos?offset={offset}&limit=137')
            self.assertEqual(status, 200)
            self.assertEqual(page['total'], 2000)
            self.assertEqual(page['hasMore'], offset + len(page['items']) < 2000)
            ids.extend(photo['id'] for photo in page['items'])
        self.assertEqual(ids, list(range(1, 2001)))
        for offset in (2000, 3000):
            _, page = self.get(f'/modules/1/photos?offset={offset}')
            self.assertEqual(page['items'], [])
            self.assertFalse(page['hasMore'])
        self.assertEqual(self.get('/modules/1/photos'), self.get('/modules/1/photos'))
        _, last = self.get('/modules/1600/photos?offset=1999&limit=200')
        self.assertEqual(last['items'][0]['id'], 3200000)

    def test_errors(self):
        for path in ('/platforms/0/modules', '/platforms/41/modules',
                     '/modules/0/photos', '/modules/1601/photos', '/unknown'):
            self.assertEqual(self.get(path)[0], 404)
        for query in ('offset=-1', 'offset=abc', 'limit=0', 'limit=201',
                      'limit=', 'limit=1&limit=2'):
            self.assertEqual(self.get('/modules/1/photos?' + query)[0], 400)


if __name__ == '__main__':
    unittest.main()
