const test = require('node:test');
const assert = require('node:assert/strict');
const { metrics } = require('./evaluate.cjs');
test('metrics account for ranking, incomplete recall and graded relevance', () => {
 const result = metrics([{id:1},{id:2},{id:3}], {'1':3,'3':1,'4':2});
 assert.equal(result.precision, 2/3);
 assert.equal(result.recall, 2/3);
 assert.ok(result.ndcg > 0 && result.ndcg < 1);
});
test('perfect ranking and no relevant items are handled', () => {
 assert.equal(metrics([{id:1},{id:2}], {'1':3,'2':1}).ndcg,1);
 assert.deepEqual(metrics([{id:1}], {}), {precision:0,recall:0,ndcg:0});
});

test('within-list diversity counts distinct categories', () => {
 const {withinListDiversity}=require('./evaluate.cjs');
 assert.equal(withinListDiversity([{theme:'A'},{theme:'A'},{theme:'B'}],'theme'),2/3);
 assert.equal(withinListDiversity([],'theme'),0);
});
