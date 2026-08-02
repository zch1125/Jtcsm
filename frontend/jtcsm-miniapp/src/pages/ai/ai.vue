<template>
  <view class="page">
    <!-- 顶部切换：对话 / 历史 -->
    <view class="top-tabs">
      <view class="top-tab" :class="{ active: currentView === 'chat' }" @tap="switchView('chat')">AI 对话</view>
      <view class="top-tab" :class="{ active: currentView === 'history' }" @tap="switchView('history')">生成历史</view>
    </view>

    <!-- 对话视图 -->
    <view v-show="currentView === 'chat'" class="chat-container">
      <scroll-view class="messages" scroll-y :scroll-into-view="scrollTarget" :scroll-with-animation="true">
        <view v-for="(msg, mi) in messages" :key="mi" :id="'m-' + mi" class="msg-wrapper">

          <!-- 用户消息 -->
          <view v-if="msg.role === 'user'" class="msg-row user-row">
            <view class="msg-bubble user-bubble">
              <text class="msg-text">{{ msg.content }}</text>
            </view>
          </view>

          <!-- AI 消息 -->
          <view v-if="msg.role === 'ai'" class="msg-row ai-row">
            <view class="ai-avatar">AI</view>
            <view class="ai-body">
              <text class="ai-label">AI 推荐菜谱</text>

              <!-- 多道菜谱卡片 -->
              <view
                v-for="(rc, ri) in msg.recipes"
                :key="ri"
                class="recipe-card"
                :class="{ expanded: msg.expanded === ri }"
                @tap="toggleExpand(mi, ri)"
              >
                <!-- 卡片头部 -->
                <view class="rc-header">
                  <view class="rc-name-row">
                    <text class="rc-name">{{ rc.name }}</text>
                    <text class="rc-icon">{{ msg.expanded === ri ? '▾' : '▸' }}</text>
                  </view>
                  <view class="rc-meta">
                    <text class="rc-tag">{{ rc.cuisine }}</text>
                    <text class="rc-tag">{{ rc.difficulty }}</text>
                    <text class="rc-tag">{{ rc.cookTime }}分</text>
                  </view>
                </view>

                <!-- 展开详情 -->
                <view v-if="msg.expanded === ri" class="rc-detail">
                  <view class="rc-section">
                    <text class="rc-section-title">用料清单</text>
                    <view v-for="(ing, ii) in rc.ingredients" :key="ii" class="ing-row">
                      <text class="ing-name">{{ ing.name }}</text>
                      <text class="ing-amount">{{ ing.amount }}</text>
                    </view>
                  </view>
                  <view class="rc-section">
                    <text class="rc-section-title">烹饪步骤</text>
                    <view v-for="(st, si) in rc.steps" :key="si" class="step-row">
                      <view class="step-no">{{ st.stepNo }}</view>
                      <text class="step-content">{{ st.content }}</text>
                    </view>
                  </view>
                </view>
              </view>
            </view>
          </view>
        </view>

        <!-- AI 输入提示 -->
        <view v-if="loading" id="m-loading" class="msg-wrapper">
          <view class="msg-row ai-row">
            <view class="ai-avatar">AI</view>
            <view class="ai-body">
              <view class="thinking">AI 正在思考...</view>
            </view>
          </view>
        </view>
      </scroll-view>

      <!-- 快捷建议 -->
      <scroll-view v-if="showSuggestions" class="suggestions" scroll-x>
        <text v-for="(s, si) in suggestions" :key="si" class="suggestion-tag" @tap="quickSuggest(s)">{{ s }}</text>
      </scroll-view>

      <!-- 输入栏 -->
      <view class="input-bar">
        <input
          class="chat-input"
          v-model="userInput"
          placeholder="输入食材和要求..."
          confirm-type="send"
          @confirm="sendMessage"
          :disabled="loading"
        />
        <view class="send-btn" :class="{ disabled: loading }" @tap="sendMessage">发送</view>
      </view>
    </view>

    <!-- 历史视图 -->
    <view v-show="currentView === 'history'" class="history-container">
      <view class="history-list" v-if="historyList.length > 0">
        <view class="history-item" v-for="h in historyList" :key="h.id" @tap="loadHistoryItem(h)">
          <view class="hi-top">
            <text class="hi-mode">{{ modeLabel(h.mode) }}</text>
            <text class="hi-time">{{ formatTime(h.createdAt) }}</text>
            <text class="hi-rating" v-if="h.rating">{{ h.rating }}分</text>
          </view>
          <text class="hi-input">{{ h.inputContent }}</text>
        </view>
      </view>
      <view class="empty" v-else>
        <text class="empty-text">还没有生成记录，开始对话吧</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { aiGenerate, getAiHistory, saveAiToFavorite } from '../../api/index'

const currentView = ref<'chat' | 'history'>('chat')

interface RecipeItem {
  name: string
  cuisine: string
  difficulty: string
  cookTime: number
  ingredients: { name: string; amount: string }[]
  steps: { stepNo: number; content: string; duration: number }[]
}

interface Message {
  role: 'user' | 'ai'
  content?: string
  recipes?: RecipeItem[]
  expanded?: number
}

const messages = ref<Message[]>([])
const userInput = ref('')
const loading = ref(false)
const scrollTarget = ref('')
const showSuggestions = ref(true)

const suggestions = [
  '鸡蛋 番茄 少油',
  '土豆 牛肉 红烧',
  '麻辣口味 有什么推荐',
  '豆腐 鸡蛋 清淡',
  '鸡翅 可乐',
  '排骨 糖醋',
]

const COMMON_INGREDIENTS = [
  '鸡蛋', '番茄', '土豆', '牛肉', '猪肉', '鸡肉', '豆腐',
  '鱼', '虾', '排骨', '鸡翅', '茄子', '青椒', '白菜',
  '包菜', '西兰花', '萝卜', '胡萝卜', '黄瓜', '洋葱',
  '葱', '姜', '蒜', '辣椒', '玉米', '豆角', '蘑菇', '木耳',
  '冬瓜', '南瓜', '豆芽', '芹菜', '香菜', '韭菜', '菠菜',
  '面条', '大米', '粉丝', '可乐', '羊肉',
]

async function sendMessage() {
  const text = userInput.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  userInput.value = ''
  showSuggestions.value = false
  scrollToBottom()

  loading.value = true
  try {
    const { mode, ingredients, conditions, name } = parseInput(text)
    const params: any = { mode }

    if (mode === 'ingredients') {
      params.ingredients = ingredients
      if (conditions) params.conditions = conditions
    } else {
      params.name = name || text
      if (conditions) params.conditions = conditions
    }

    const res = await aiGenerate(params)

    if (res.recipes && res.recipes.length > 0) {
      messages.value.push({
        role: 'ai',
        recipes: res.recipes,
        expanded: -1,
      })
    } else {
      messages.value.push({ role: 'ai', recipes: [], expanded: -1 })
      // push an empty AI message when no recipes
    }
  } catch (e) {
    messages.value.push({ role: 'ai', recipes: [], expanded: -1 })
    uni.showToast({ title: 'AI 生成失败，请重试', icon: 'none' })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function parseInput(text: string) {
  const found: string[] = []
  const rest: string[] = []

  const tokens = text.split(/[\s,，、。.；;！!？?]+/).filter(Boolean)

  for (const token of tokens) {
    const matched = COMMON_INGREDIENTS.find((ci) => token.includes(ci))
    if (matched && !found.includes(matched)) {
      found.push(matched)
    } else if (!COMMON_INGREDIENTS.some((ci) => token.includes(ci))) {
      rest.push(token)
    }
  }

  if (found.length > 0) {
    return {
      mode: 'ingredients',
      ingredients: found,
      conditions: rest.join('，'),
      name: null,
    }
  }

  return {
    mode: 'name',
    ingredients: [],
    conditions: '',
    name: tokens.slice(0, 5).join(''),
  }
}

function toggleExpand(msgIdx: number, recipeIdx: number) {
  const msg = messages.value[msgIdx]
  if (!msg || msg.role !== 'ai') return
  msg.expanded = msg.expanded === recipeIdx ? -1 : recipeIdx
  messages.value = [...messages.value]
  nextTick(() => scrollToBottom())
}

function quickSuggest(text: string) {
  userInput.value = text
  sendMessage()
}

function scrollToBottom() {
  nextTick(() => {
    const id = loading.value ? 'm-loading' : 'm-' + (messages.value.length - 1)
    scrollTarget.value = id
  })
}

function switchView(v: 'chat' | 'history') {
  currentView.value = v
  if (v === 'history') fetchHistory()
}

// ==================== 历史 ====================

interface HistoryRecord {
  id: number
  mode: string
  inputContent: string
  resultJson: string
  rating: number | null
  feedback: string | null
  createdAt: string
}

const historyList = ref<HistoryRecord[]>([])

async function fetchHistory() {
  try {
    historyList.value = await getAiHistory(1, 50)
  } catch (e) {}
}

function modeLabel(m: string): string {
  const map: Record<string, string> = { ingredients: '按食材', name: '按菜名', creative: '创意融合' }
  return map[m] || m
}

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n: number) => (n < 10 ? '0' + n : '' + n)
  return pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
}

function loadHistoryItem(h: HistoryRecord) {
  try {
    const parsed = JSON.parse(h.resultJson)
    if (Array.isArray(parsed) && parsed.length > 0) {
      currentView.value = 'chat'
      messages.value.push({ role: 'ai', recipes: parsed, expanded: -1 })
      scrollToBottom()
      uni.showToast({ title: '已加载', icon: 'success' })
    }
  } catch (e) {}
}
</script>

<style lang="scss" scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f2f2f7;
}

.top-tabs {
  display: flex;
  background: #fff;
  border-bottom: 1rpx solid #e5e5e5;
  flex-shrink: 0;
}
.top-tab {
  flex: 1;
  text-align: center;
  padding: 24rpx 0;
  font-size: 28rpx;
  color: #666;
  position: relative;
  &.active {
    color: #e74c3c;
    font-weight: bold;
    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 48rpx;
      height: 4rpx;
      background: #e74c3c;
      border-radius: 2rpx;
    }
  }
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.messages {
  flex: 1;
  padding: 20rpx;
}
.msg-wrapper {
  margin-bottom: 20rpx;
}
.msg-row {
  display: flex;
  margin-bottom: 8rpx;
}

.user-row {
  justify-content: flex-end;
}
.user-bubble {
  max-width: 75%;
  background: #e74c3c;
  color: #fff;
  padding: 16rpx 24rpx;
  border-radius: 16rpx 16rpx 4rpx 16rpx;
  font-size: 28rpx;
  line-height: 1.5;
}

.ai-row {
  align-items: flex-start;
}
.ai-avatar {
  width: 56rpx;
  height: 56rpx;
  line-height: 56rpx;
  text-align: center;
  background: linear-gradient(135deg, #e74c3c, #c0392b);
  color: #fff;
  border-radius: 50%;
  font-size: 22rpx;
  font-weight: bold;
  flex-shrink: 0;
  margin-right: 12rpx;
}
.ai-body {
  flex: 1;
  max-width: 82%;
}
.ai-label {
  font-size: 22rpx;
  color: #999;
  margin-bottom: 8rpx;
  display: block;
}

.thinking {
  color: #999;
  font-size: 26rpx;
}

/* Recipe cards */
.recipe-card {
  background: #fff;
  border-radius: 12rpx;
  padding: 20rpx;
  margin-bottom: 10rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.06);
}
.rc-header {
  display: flex;
  flex-direction: column;
}
.rc-name-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.rc-name {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
}
.rc-icon {
  font-size: 28rpx;
  color: #999;
}
.rc-meta {
  display: flex;
  gap: 8rpx;
  margin-top: 8rpx;
  flex-wrap: wrap;
}
.rc-tag {
  font-size: 22rpx;
  color: #e74c3c;
  background: #fef0f0;
  padding: 2rpx 12rpx;
  border-radius: 4rpx;
}
.rc-detail {
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #f0f0f0;
}
.rc-section {
  margin-bottom: 12rpx;
}
.rc-section-title {
  font-size: 26rpx;
  font-weight: bold;
  color: #555;
  display: block;
  margin-bottom: 8rpx;
}
.ing-row {
  display: flex;
  justify-content: space-between;
  padding: 4rpx 0;
  border-bottom: 1rpx solid #f8f8f8;
}
.ing-name { font-size: 26rpx; color: #333; }
.ing-amount { font-size: 24rpx; color: #999; }
.step-row {
  display: flex;
  margin-bottom: 10rpx;
}
.step-no {
  width: 36rpx; height: 36rpx; line-height: 36rpx; text-align: center;
  background: #e74c3c; color: #fff; border-radius: 50%;
  font-size: 20rpx; flex-shrink: 0; margin-right: 10rpx;
}
.step-content {
  font-size: 26rpx; color: #333; line-height: 1.5; flex: 1;
}

/* Suggestions */
.suggestions {
  white-space: nowrap;
  padding: 0 20rpx 12rpx;
  flex-shrink: 0;
}
.suggestion-tag {
  display: inline-block;
  font-size: 24rpx; color: #e74c3c; background: #fef0f0;
  padding: 8rpx 24rpx; border-radius: 28rpx; margin-right: 12rpx;
}

/* Input bar */
.input-bar {
  display: flex;
  align-items: center;
  padding: 16rpx 20rpx;
  padding-bottom: 36rpx;
  background: #fff;
  border-top: 1rpx solid #e5e5e5;
  flex-shrink: 0;
}
.chat-input {
  flex: 1; height: 64rpx; background: #f5f5f5; border-radius: 32rpx;
  padding: 0 24rpx; font-size: 28rpx;
}
.send-btn {
  margin-left: 16rpx; height: 64rpx; line-height: 64rpx; padding: 0 28rpx;
  background: #e74c3c; color: #fff; border-radius: 32rpx;
  font-size: 28rpx; border: none; flex-shrink: 0;
  &.disabled { background: #ccc; }
}

/* History */
.history-container { flex: 1; padding: 20rpx; }
.history-list { display: flex; flex-direction: column; gap: 12rpx; }
.history-item { background: #fff; border-radius: 12rpx; padding: 20rpx; }
.hi-top { display: flex; align-items: center; gap: 12rpx; margin-bottom: 8rpx; }
.hi-mode { font-size: 22rpx; color: #e74c3c; background: #fef0f0; padding: 2rpx 12rpx; border-radius: 4rpx; }
.hi-time { font-size: 22rpx; color: #bbb; flex: 1; }
.hi-rating { font-size: 22rpx; color: #ffa500; }
.hi-input { font-size: 26rpx; color: #666; display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.empty { display: flex; justify-content: center; padding-top: 200rpx; }
.empty-text { font-size: 28rpx; color: #999; }
</style>
