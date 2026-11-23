<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { GraphChart } from 'echarts/charts'
import { LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
echarts.use([GraphChart, LegendComponent, TooltipComponent, CanvasRenderer])

const props = defineProps({ graph: { type: Object, required: true }, relationNames: { type: Object, required: true } })
const emit = defineEmits(['choose-interest'])
const chartEl = ref(null), selected = ref(null)
const categories = ['景点', '地区', '历史事件', '人物', '著作', '主题']
const colors = ['#984236', '#b18b60', '#c27a43', '#657e70', '#7b7091', '#a79b68']
let chart, observer
const nodeById = computed(() => new Map(props.graph.nodes.map(node => [node.id, node])))
const selectedEdges = computed(() => !selected.value ? [] : props.graph.edges.filter(edge => edge.source === selected.value.id || edge.target === selected.value.id))
const canChoose = computed(() => selected.value && ['人物', '历史事件', '著作', '主题'].includes(selected.value.label))
function render() {
  if (!chart) return
  const categoryData = categories.map((name, i) => ({ name, itemStyle: { color: colors[i] } }))
  chart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', renderMode: 'richText', formatter: params => params.dataType === 'node' ? `${params.data.name}\n${params.data.kind}` : (props.relationNames[params.data.type] || params.data.type) },
    legend: [{ data: categories, top: 0, left: 'center', itemWidth: 10, itemHeight: 10, textStyle: { color: '#76685b', fontSize: 11 } }],
    animationDurationUpdate: 350,
    series: [{ type: 'graph', layout: 'force', zoom: 0.65, left: 100, right: 155, top: 55, bottom: 48, roam: true, draggable: true, force: { repulsion: 520, edgeLength: [100, 165], gravity: .12 },
      categories: categoryData, data: props.graph.nodes.map(n => ({ id: n.id, name: n.name, kind: n.label, category: Math.max(0, categories.indexOf(n.label)), symbolSize: n.label === '景点' ? 35 : 24 })),
      links: props.graph.edges.map(e => ({ source: e.source, target: e.target, type: e.type })),
      label: { show: true, position: 'right', color: '#55483e', fontSize: 11 },
      lineStyle: { color: '#bfa99a', width: 1.3, opacity: .75, curveness: .08 },
      edgeSymbol: ['none', 'arrow'], edgeSymbolSize: [0, 7],
      emphasis: { focus: 'adjacency', lineStyle: { width: 2.5, opacity: 1 } }
    }]
  }, { notMerge: true })
}
function selectNode(id) { selected.value = nodeById.value.get(id) || null }
function resetView() { render(); selected.value = null }
onMounted(async () => {
  await nextTick(); chart = echarts.init(chartEl.value); chart.on('click', params => { if (params.dataType === 'node') selectNode(params.data.id) })
  observer = new ResizeObserver(() => chart?.resize()); observer.observe(chartEl.value); render()
})
watch(() => props.graph, () => { selected.value = null; render() })
onBeforeUnmount(() => { observer?.disconnect(); chart?.dispose(); chart = null })
</script>
<template>
  <div class="graph-toolstrip"><p>拖动节点调整布局，滚轮缩放，点击节点查看关系与来源。</p><button @click="resetView">重置视图</button></div>
  <div class="graph-layout"><div ref="chartEl" class="graph-canvas" role="img" aria-label="可拖动和缩放的知识关系图；也可通过下方节点列表查看关系"></div>
    <aside class="node-inspector"><template v-if="selected"><span class="inspector-kind">{{ selected.label }}</span><h4>{{ selected.name }}</h4><a v-if="selected.sourceUrl" :href="selected.sourceUrl" target="_blank" rel="noopener noreferrer">实体资料来源 ↗</a><button v-if="canChoose" class="interest-action" @click="emit('choose-interest', selected.id)">用作推荐兴趣 ↗</button><h5>相关关系</h5><ul><li v-for="edge in selectedEdges" :key="edge.source + edge.type + edge.target"><span>{{ nodeById.get(edge.source)?.name }} · {{ relationNames[edge.type] || edge.type }} · {{ nodeById.get(edge.target)?.name }}</span> <a v-if="edge.sourceUrl" :href="edge.sourceUrl" target="_blank" rel="noopener noreferrer">来源 ↗</a></li></ul><p v-if="!selectedEdges.length">暂无关系。</p></template><template v-else><span class="inspector-kind">探索图谱</span><h4>选择一个节点</h4><p>点击图中的景点、人物或事件，查看其关联与资料出处。</p></template></aside>
  </div>
  <details class="graph-access"><summary>节点与关系清单（可使用键盘操作）</summary><div class="node-buttons"><button v-for="node in graph.nodes" :key="node.id" @click="selectNode(node.id)">{{ node.name }} <small>{{ node.label }}</small></button></div><div class="graph-relations"><div v-for="edge in graph.edges" :key="edge.source + edge.type + edge.target" class="graph-relation"><span>{{ nodeById.get(edge.source)?.name }}</span><span class="graph-arrow">{{ relationNames[edge.type] || edge.type }} →</span><span>{{ nodeById.get(edge.target)?.name }}</span><a v-if="edge.sourceUrl" :href="edge.sourceUrl" target="_blank" rel="noopener noreferrer">查看来源 ↗</a></div></div></details>
</template>


