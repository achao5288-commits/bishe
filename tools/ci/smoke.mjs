import { randomBytes } from 'node:crypto';
import { pathToFileURL } from 'node:url';

export async function runSmoke(base, fetcher = fetch) {
  const request = async (path, options = {}) => {
    const response = await fetcher(new URL(path, base), { signal: AbortSignal.timeout(15000), ...options });
    if (!response.ok) throw new Error(`${path}: HTTP ${response.status}`);
    return response;
  };
  const json = async (path, options) => (await request(path, options)).json();
  const requireValue = (condition, message) => { if (!condition) throw new Error(message); };
  requireValue((await (await request('/')).text()).includes('id="app"'), 'Frontend HTML missing app root');
  requireValue((await json('/api/health')).status === 'UP', 'Backend not ready');
  const attractions = await json('/api/attractions');
  requireValue(Array.isArray(attractions) && attractions.length > 0 && Number.isInteger(attractions[0].id), 'Attractions missing');
  const graph = await json('/api/graph');
  requireValue(graph.nodes?.length > 0 && graph.edges?.length > 0, 'Graph is empty');
  const post = body => ({ method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
  const recommendations = await json('/api/recommendations', post({ limit: 3 }));
  requireValue(Array.isArray(recommendations) && recommendations.length > 0, 'Recommendations missing');
  const credentials = { username: `ci_${randomBytes(8).toString('hex')}`, password: 'ci-test-password-only' };
  const account = await json('/api/auth/register', post(credentials));
  requireValue(typeof account.token === 'string' && account.token.length > 0 && account.role === 'USER', 'Registration failed');
  const headers = { Authorization: `Bearer ${account.token}` };
  await request(`/api/me/favorites/${attractions[0].id}`, { method: 'PUT', headers });
  const profile = await json('/api/me/profile', { headers });
  requireValue(profile.favorites?.includes(attractions[0].id), 'Favorite was not persisted');
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const base = process.argv[2] || 'http://127.0.0.1:8080';
  runSmoke(base).then(() => console.log('Compose smoke checks passed')).catch(error => {
    console.error(`Compose smoke checks failed: ${error.message}`);
    process.exitCode = 1;
  });
}
