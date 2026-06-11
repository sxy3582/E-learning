<template>
  <section class="grid grid-2">
    <article class="card">
      <h2 class="title">算法可视化实验室</h2>
      <p style="color: #7a5a37;">支持：冒泡排序、选择排序、插入排序、二分查找、线性查找。小屏可浏览，建议使用 PC 获得最佳体验。</p>
      <div class="form-grid">
        <select v-model="algorithm" @change="switchAlgorithm">
          <option value="bubble">冒泡排序</option>
          <option value="selection">选择排序</option>
          <option value="insertion">插入排序</option>
          <option value="binary">二分查找</option>
          <option value="linear">线性查找</option>
        </select>
        <label v-if="isSearchAlgorithm" class="inline-field">
          <span>查找目标值</span>
          <input v-model.number="target" type="number" placeholder="例如 44" />
        </label>
      </div>
      <p v-if="isSearchAlgorithm" class="muted">当前要查找的数：<strong>{{ target }}</strong>。红色柱子表示当前正在比较或指针所在位置。</p>
      <div style="display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 12px;">
        <button class="secondary" @click="randomize">随机数据</button>
        <button class="primary" @click="start" :disabled="running">开始</button>
        <button class="secondary" @click="pause" :disabled="!running">暂停</button>
        <button class="secondary" @click="nextStep">下一步</button>
        <button class="secondary" @click="reset">重置</button>
      </div>
      <label>速度（毫秒）：{{ speed }}</label>
      <input type="range" min="100" max="1200" step="100" v-model.number="speed" />
      <div class="algorithm-bars">
        <div class="algorithm-bar-wrap" v-for="(num, idx) in arr" :key="idx">
          <span class="bar-value">{{ num }}</span>
          <div :style="barStyle(num, idx)" :title="`值：${num}`" />
          <small v-if="isSearchAlgorithm" class="bar-index">{{ idx }}</small>
        </div>
      </div>
    </article>
    <article class="card">
      <h3 class="title">步骤说明</h3>
      <p>{{ message }}</p>
      <p v-if="algorithm === 'binary'" class="muted">数组已按从小到大排序，二分查找每一步会比较 mid 位置的数和目标值 {{ target }}。</p>
      <p v-if="algorithm === 'linear'" class="muted">线性查找会从左到右逐个比较，直到找到目标值 {{ target }} 或遍历结束。</p>
      <pre v-if="algorithm === 'bubble'" style="white-space: pre-wrap;">for i = 0 to n-1
  for j = 0 to n-1-i
    if arr[j] > arr[j+1]
      swap(arr[j], arr[j+1])</pre>
      <pre v-else-if="algorithm === 'selection'" style="white-space: pre-wrap;">for i = 0 to n-1
  minIdx = i
  for j = i+1 to n-1
    if arr[j] < arr[minIdx]
      minIdx = j
  swap(arr[i], arr[minIdx])</pre>
      <pre v-else-if="algorithm === 'insertion'" style="white-space: pre-wrap;">for i = 1 to n-1
  key = arr[i]
  j = i - 1
  while j >= 0 and arr[j] > key
    arr[j+1] = arr[j]
    j--
  arr[j+1] = key</pre>
      <pre v-else-if="algorithm === 'binary'" style="white-space: pre-wrap;">left = 0, right = n - 1
while left <= right
  mid = (left + right) / 2
  if arr[mid] == target: found
  if arr[mid] < target: left = mid + 1
  else: right = mid - 1</pre>
      <pre v-else style="white-space: pre-wrap;">for i = 0 to n-1
  if arr[i] == target
    return i
return -1</pre>
    </article>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();
const allowedAlgorithms = ["bubble", "selection", "insertion", "binary", "linear"];
const initialAlgorithm = allowedAlgorithms.includes(route.query.algo) ? route.query.algo : "bubble";
const algorithm = ref(initialAlgorithm);
const arr = ref([]);
const original = ref([]);
const i = ref(0);
const j = ref(0);
const minIdx = ref(0);
const left = ref(0);
const right = ref(0);
const mid = ref(-1);
const target = ref(0);
const running = ref(false);
const speed = ref(500);
const message = ref("点击“随机数据”并开始。");
const keyValue = ref(null);
let timer = null;

const isSearchAlgorithm = computed(() => algorithm.value === "binary" || algorithm.value === "linear");

randomize();

function randomize() {
  stopTimer();
  let values = Array.from({ length: 12 }, () => Math.floor(Math.random() * 90) + 10);
  if (algorithm.value === "binary") {
    values = values.sort((a, b) => a - b);
    target.value = values[Math.floor(values.length / 2)];
  } else if (algorithm.value === "linear") {
    target.value = values[Math.floor(values.length / 2)];
  }
  arr.value = values;
  original.value = [...values];
  i.value = 0;
  j.value = 0;
  minIdx.value = 0;
  left.value = 0;
  right.value = values.length - 1;
  mid.value = -1;
  keyValue.value = null;
  message.value = `数据已生成，准备开始${algorithmLabel()}。`;
}

function start() {
  if (running.value) {
    return;
  }
  running.value = true;
  tick();
}

function tick() {
  if (!running.value) {
    return;
  }
  const done = stepOnce();
  if (done) {
    running.value = false;
    return;
  }
  timer = setTimeout(tick, speed.value);
}

function pause() {
  running.value = false;
  stopTimer();
}

function nextStep() {
  pause();
  stepOnce();
}

function stepOnce() {
  if (algorithm.value === "selection") {
    return stepSelection();
  }
  if (algorithm.value === "insertion") {
    return stepInsertion();
  }
  if (algorithm.value === "binary") {
    return stepBinary();
  }
  if (algorithm.value === "linear") {
    return stepLinear();
  }
  return stepBubble();
}

function stepBubble() {
  const n = arr.value.length;
  if (i.value >= n - 1) {
    message.value = "排序完成。";
    return true;
  }
  if (j.value >= n - 1 - i.value) {
    i.value += 1;
    j.value = 0;
    message.value = `第 ${i.value} 轮完成，进入下一轮。`;
    return false;
  }
  const a = arr.value[j.value];
  const b = arr.value[j.value + 1];
  if (a > b) {
    arr.value[j.value] = b;
    arr.value[j.value + 1] = a;
    message.value = `比较 ${a} 和 ${b}，发生交换。`;
  } else {
    message.value = `比较 ${a} 和 ${b}，无需交换。`;
  }
  j.value += 1;
  return false;
}

function stepSelection() {
  const n = arr.value.length;
  if (i.value >= n - 1) {
    message.value = "选择排序完成。";
    return true;
  }
  if (j.value === 0) {
    minIdx.value = i.value;
    j.value = i.value + 1;
  }
  if (j.value < n) {
    if (arr.value[j.value] < arr.value[minIdx.value]) {
      minIdx.value = j.value;
      message.value = `发现更小值 ${arr.value[minIdx.value]}，更新最小下标。`;
    } else {
      message.value = `比较 ${arr.value[j.value]} 和当前最小值 ${arr.value[minIdx.value]}。`;
    }
    j.value += 1;
    return false;
  }
  if (minIdx.value !== i.value) {
    const tmp = arr.value[i.value];
    arr.value[i.value] = arr.value[minIdx.value];
    arr.value[minIdx.value] = tmp;
    message.value = `第 ${i.value + 1} 轮选择完成，执行交换。`;
  } else {
    message.value = `第 ${i.value + 1} 轮选择完成，无需交换。`;
  }
  i.value += 1;
  j.value = 0;
  return false;
}

function stepInsertion() {
  const n = arr.value.length;
  if (i.value === 0) {
    i.value = 1;
    j.value = 0;
    keyValue.value = arr.value[i.value];
  }
  if (i.value >= n) {
    message.value = "插入排序完成。";
    return true;
  }
  if (keyValue.value === null) {
    keyValue.value = arr.value[i.value];
    j.value = i.value - 1;
    message.value = `取出 ${keyValue.value}，准备插入到左侧有序区间。`;
    return false;
  }
  if (j.value >= 0 && arr.value[j.value] > keyValue.value) {
    message.value = `${arr.value[j.value]} > ${keyValue.value}，向右移动一位。`;
    arr.value[j.value + 1] = arr.value[j.value];
    j.value -= 1;
    return false;
  }
  arr.value[j.value + 1] = keyValue.value;
  message.value = `将 ${keyValue.value} 插入到下标 ${j.value + 1}。`;
  i.value += 1;
  keyValue.value = null;
  return false;
}

function stepBinary() {
  if (left.value > right.value) {
    message.value = `查找结束，未找到 ${target.value}。`;
    return true;
  }
  mid.value = Math.floor((left.value + right.value) / 2);
  const value = arr.value[mid.value];
  if (value === target.value) {
    message.value = `找到目标值 ${target.value}，下标为 ${mid.value}。`;
    return true;
  }
  if (value < target.value) {
    message.value = `${value} < ${target.value}，移动 left。`;
    left.value = mid.value + 1;
  } else {
    message.value = `${value} > ${target.value}，移动 right。`;
    right.value = mid.value - 1;
  }
  return false;
}

function stepLinear() {
  if (i.value >= arr.value.length) {
    message.value = `查找结束，未找到 ${target.value}。`;
    return true;
  }
  const value = arr.value[i.value];
  if (value === target.value) {
    message.value = `找到目标值 ${target.value}，下标为 ${i.value}。`;
    return true;
  }
  message.value = `比较下标 ${i.value} 的值 ${value}，不是目标值 ${target.value}，继续向右查找。`;
  i.value += 1;
  return false;
}

function reset() {
  pause();
  arr.value = [...original.value];
  i.value = 0;
  j.value = 0;
  minIdx.value = 0;
  left.value = 0;
  right.value = arr.value.length - 1;
  mid.value = -1;
  keyValue.value = null;
  message.value = "已重置到初始数组。";
}

function barStyle(num, idx) {
  let active = idx === j.value || idx === j.value + 1;
  if (algorithm.value === "selection") {
    active = idx === j.value || idx === minIdx.value || idx === i.value;
  }
  if (algorithm.value === "binary") {
    active = idx === mid.value || idx === left.value || idx === right.value;
  }
  if (algorithm.value === "insertion") {
    active = idx === i.value || idx === j.value || idx === j.value + 1;
  }
  if (algorithm.value === "linear") {
    active = idx === i.value;
  }
  return {
    width: "26px",
    height: `${num * 2}px`,
    background: active ? "#b93b29" : "#1e2a39",
    borderRadius: "8px 8px 0 0"
  };
}

function stopTimer() {
  if (timer) {
    clearTimeout(timer);
    timer = null;
  }
}

function switchAlgorithm() {
  randomize();
}

function algorithmLabel() {
  if (algorithm.value === "selection") {
    return "选择排序";
  }
  if (algorithm.value === "binary") {
    return "二分查找";
  }
  if (algorithm.value === "insertion") {
    return "插入排序";
  }
  if (algorithm.value === "linear") {
    return "线性查找";
  }
  return "冒泡排序";
}

onBeforeUnmount(() => {
  stopTimer();
});
</script>
