import { test } from 'node:test';
import assert from 'node:assert/strict';
import { runSmoke } from './smoke.mjs';

function fakeFetch(brokenGraph = false) {
  const calls = [];
  const fetcher = async (url, options = {}) => {
    const path = new URL(url).pathname;
    calls.push({ path, options });
    const bodies = {
      '/': '<html><div id="app"></div></html>',
      '/api/health': { status: 'UP' },
      '/api/attractions': [{ id: 1, name: '景点' }],
      '/api/graph': brokenGraph ? { nodes: [], edges: [] } : { nodes: [{ id: '1' }], edges: [{ source: '1', target: '2' }] },
      '/api/recommendations': [{ id: 1 }],
      '/api/auth/register': { token: 'test-token', role: 'USER' },
      '/api/me/favorites/1': 'saved',
      '/api/me/profile': { favorites: [1], history: [] },
    };
    return new Response(typeof bodies[path] === 'string' ? bodies[path] : JSON.stringify(bodies[path]), { status: bodies[path] ? 200 : 404 });
  };
  return { fetcher, calls };
}

test('checks frontend, graph, recommendation and authenticated storage through the same proxy', async () => {
  const { fetcher, calls } = fakeFetch();
  await runSmoke('http://127.0.0.1:8080', fetcher);
  assert.equal(calls.length, 8);
  assert.equal(calls.find(x => x.path === '/api/me/profile').options.headers.Authorization, 'Bearer test-token');
});

test('fails when graph data is empty', async () => {
  await assert.rejects(runSmoke('http://127.0.0.1:8080', fakeFetch(true).fetcher), /graph/i);
});
