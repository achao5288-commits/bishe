import {test} from 'node:test';
import assert from 'node:assert/strict';
import {normalizeState,toggleFavorite,recordView,buildProfile} from './src/profile.js';
const graph={nodes:[{id:'event-a',name:'会议',label:'历史事件'},{id:'person-b',name:'人物',label:'人物'}],edges:[{source:'attraction-1',target:'event-a',type:'RELATED_TO_EVENT'},{source:'attraction-1',target:'person-b',type:'RELATED_TO_PERSON'},{source:'attraction-2',target:'event-a',type:'RELATED_TO_EVENT'},{source:'attraction-1',target:'event-a',type:'RELATED_TO_EVENT'}]};
test('favorites toggle, views deduplicate and move to front',()=>{let s=toggleFavorite({},1);s=recordView(recordView(s,2),1);s=recordView(s,2);assert.deepEqual(s,{favorites:[1],history:[2,1]});assert.deepEqual(toggleFavorite(s,1).favorites,[]);});
test('profile is bounded and duplicate graph edges do not inflate weight',()=>{assert.deepEqual(buildProfile({favorites:[1],history:[1,2]},graph),[{id:'event-a',name:'会议',weight:3},{id:'person-b',name:'人物',weight:3}]);assert.deepEqual(buildProfile({history:[1]},graph),[{id:'event-a',name:'会议',weight:1},{id:'person-b',name:'人物',weight:1}]);});
test('corrupt and malformed state is safely normalized',()=>{assert.deepEqual(normalizeState({favorites:[1,1,'2',-1],history:null}),{favorites:[1],history:[]});});
