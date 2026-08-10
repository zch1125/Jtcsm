<template>
  <div class="ai-page">
    <div class="ai-shell card">
      <div class="ai-tabs">
        <button class="ai-tab" :class="{ active: currentView === 'chat' }" @click="switchView('chat')">
          AI 对话
        </button>
        <button class="ai-tab" :class="{ active: currentView === 'history' }" @click="switchView('history')">
          生成历史
        </button>
      </div>

      <div v-show="currentView === 'chat'" class="chat-area">
        <div ref="messagesEl" class="messages">
          <div v-if="!messages.length" class="chat-welcome">
            <div class="welcome-icon">AI</div>
            <h2>今天想吃什么？</h2>
            <p>输入食材、菜名或口味要求，我来帮你搭配一桌好菜。</p>
            <div class="welcome-chips">
              <button
                v-for="(s, si) in suggestions"
                :key="si"
                class="chip"
                @click="quickSuggest(s)"
              >
                {{ s }}
              </button>
            </div>
          </div>

          <div v-for="(msg, mi) in messages" :key="mi" class="msg" :class="msg.role">
            <div v-if="msg.role === 'ai'" class="ai-avatar">AI</div>
            <div class="msg-body">
              <div v-if="msg.content" class="bubble ai-bubble">{{ msg.content }}</div>
              <div v-else-if="msg.streaming" class="thinking">AI 正在思考...</div>

              <div v-if="msg.recipes?.length" class="recipe-cards">
                <article
                  v-for="(rc, ri) in msg.recipes"
                  :key="ri"
                  class="recipe-card"
                  @click="openRecipe(mi, ri)"
                >
                  <div class="rc-top">
                    <div class="rc-cover">
                      <img v-if="rc.coverImage" :src="resolveImageUrl(rc.coverImage)" :alt="rc.name" />
                      <div v-else class="rc-fallback">{{ (rc.name || "菜").slice(0, 1) }}</div>
                    </div>
                    <div class="rc-head">
                      <div class="rc-name-row">
                        <h3>{{ rc.name }}</h3>
                        <span class="rc-arrow">{{ msg.expanded === ri ? "收起" : "展开" }}</span>
                      </div>
                      <div class="rc-meta">
                        <span class="tag tag-hot">{{ rc.cuisine }}</span>
                        <span class="tag">{{ rc.difficulty }}</span>
                        <span class="tag tag-muted">{{ rc.cookTime }} 分钟</span>
                      </div>
                    </div>
                  </div>

                  <div v-if="msg.expanded === ri" class="rc-detail">
                    <div class="rc-section">
                      <h4>用料清单</h4>
                      <div v-for="(ing, ii) in rc.ingredients" :key="ii" class="rc-ing">
                        <span>{{ ing.name }}</span>
                        <span>{{ ing.amount }}</span>
                      </div>
                    </div>
                    <div class="rc-section">
                      <h4>烹饪步骤</h4>
                      <div v-for="(st, si) in rc.steps" :key="si" class="rc-step">
                        <span class="rc-step-no">{{ st.stepNo }}</span>
                        <span>{{ st.content }}</span>
                      </div>
                    </div>
                  </div>
                </article>
              </div>
            </div>
          </div>
        </div>

        <div class="input-bar">
          <input
            v-model="userInput"
            class="chat-input"
            placeholder="输入食材和需求，例如：鸡蛋 番茄 少油"
            :disabled="loading"
            @keyup.enter="sendMessage"
          />
          <button class="btn btn-primary send-btn" :disabled="loading" @click="sendMessage">
            发送
          </button>
        </div>
      </div>

      <div v-show="currentView === 'history'" class="history-area">
        <div v-if="historyList.length" class="history-list">
          <article v-for="h in historyList" :key="h.id" class="history-item" @click="loadHistoryItem(h)">
            <div class="hi-top">
              <span class="tag tag-hot">{{ modeLabel(h.mode) }}</span>
              <span class="hi-time">{{ formatTime(h.createdAt) }}</span>
              <span v-if="h.rating" class="hi-rating">{{ h.rating }} 分</span>
            </div>
            <p class="hi-input">{{ h.inputContent }}</p>
          </article>
        </div>
        <div v-else class="empty">还没有生成记录，开始一段 AI 对话吧</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref } from "vue"
import { useRouter } from "vue-router"
import { aiGenerateStream, getAiHistory, type AiGenerateParams, type AiHistoryItem } from "@/api"
import { resolveImageUrl } from "@/api/request"

const router = useRouter()
const currentView = ref<"chat" | "history">("chat")
const messagesEl = ref<HTMLElement | null>(null)

interface RecipeItem {
  recipeId?: number
  coverImage?: string
  name: string
  cuisine: string
  difficulty: string
  cookTime: number
  ingredients: Array<{ name: string; amount: string }>
  steps: Array<{ stepNo: number; content: string; duration?: number }>
}

interface Message {
  role: "user" | "ai"
  content?: string
  streaming?: boolean
  recipes?: RecipeItem[]
  expanded?: number
}

const messages = ref<Message[]>([])
const userInput = ref("")
const loading = ref(false)

const suggestions = [
  "鸡蛋 番茄 少油",
  "土豆 牛肉 红烧",
  "麻辣口味 有什么推荐",
  "豆腐 鸡蛋 清淡",
  "鸡肉 可乐",
  "排骨 糖醋"
]

const COMMON_INGREDIENTS = [
  "鸡蛋", "番茄", "西红柿", "土豆", "牛肉", "猪肉", "鸡肉", "豆腐",
  "鱼", "虾", "排骨", "鸡翅", "茄子", "青椒", "白菜",
  "包菜", "西兰花", "萝卜", "胡萝卜", "黄瓜", "洋葱",
  "葱", "姜", "蒜", "辣椒", "玉米", "豆角", "蘑菇", "木耳",
  "冬瓜", "南瓜", "豆芽", "芹菜", "香菜", "韭菜", "菠菜",
  "面条", "大米", "粉丝", "可乐", "羊肉"
]

function scrollToBottom() {
  nextTick(() => {
    if (messagesEl.value) {
      messagesEl.value.scrollTop = messagesEl.value.scrollHeight
    }
  })
}

async function sendMessage() {
  const text = userInput.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: "user", content: text })
  userInput.value = ""
  scrollToBottom()

  const aiMsg: Message = { role: "ai", content: "", streaming: true, recipes: [], expanded: -1 }
  messages.value.push(aiMsg)
  scrollToBottom()

  loading.value = true
  try {
    const { mode, ingredients, conditions, name } = parseInput(text)
    const params: AiGenerateParams = { mode }
    if (mode === "ingredients") {
      params.ingredients = ingredients
      if (conditions) params.conditions = conditions
    } else {
      params.name = name || text
      if (conditions) params.conditions = conditions
    }

    await new Promise<void>((resolve) => {
      aiGenerateStream(params, {
        onDelta: (delta) => {
          aiMsg.content = (aiMsg.content || "") + delta
          messages.value = [...messages.value]
          scrollToBottom()
        },
        onDone: (data) => {
          aiMsg.recipes = (data.recipes || []).slice(0, 8)
          aiMsg.streaming = false
          const marker = (aiMsg.content || "").indexOf("```")
          if (marker >= 0) {
            aiMsg.content = (aiMsg.content || "").substring(0, marker).trim()
          }
          if (!aiMsg.content && aiMsg.recipes?.length) {
            aiMsg.content = "已为你推荐：" + aiMsg.recipes.map((r) => r.name).join("、")
          }
          messages.value = [...messages.value]
          scrollToBottom()
          resolve()
        },
        onError: (message) => {
          aiMsg.streaming = false
          if (!aiMsg.content) aiMsg.content = message || "AI 生成失败，请重试"
          messages.value = [...messages.value]
          scrollToBottom()
          resolve()
        }
      })
    })
  } catch {
    aiMsg.streaming = false
    if (!aiMsg.content) aiMsg.content = "AI 生成失败，请重试"
    messages.value = [...messages.value]
    scrollToBottom()
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function parseInput(text: string) {
  const found: string[] = []
  const rest: string[] = []
  const tokens = text.split(/[\s,，。；;、!！?？]+/).filter(Boolean)

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
      mode: "ingredients" as const,
      ingredients: found,
      conditions: rest.join("，"),
      name: null
    }
  }

  return {
    mode: "name" as const,
    ingredients: [],
    conditions: "",
    name: tokens.slice(0, 5).join("")
  }
}

function quickSuggest(text: string) {
  userInput.value = text
  sendMessage()
}

function openRecipe(msgIdx: number, recipeIdx: number) {
  const msg = messages.value[msgIdx]
  const rc = msg?.recipes?.[recipeIdx]
  if (!rc) return
  if (rc.recipeId) {
    router.push({ path: "/recipes", query: { id: String(rc.recipeId) } })
    return
  }
  msg.expanded = msg.expanded === recipeIdx ? -1 : recipeIdx
  messages.value = [...messages.value]
  scrollToBottom()
}

function switchView(v: "chat" | "history") {
  currentView.value = v
  if (v === "history") fetchHistory()
}

// ==================== 历史 ====================

const historyList = ref<AiHistoryItem[]>([])

async function fetchHistory() {
  try {
    historyList.value = await getAiHistory(1, 50)
  } catch {
    historyList.value = []
  }
}

function modeLabel(m: string): string {
  const map: Record<string, string> = { ingredients: "按食材", name: "按菜名", creative: "创意融合" }
  return map[m] || m
}

function formatTime(dateStr: string): string {
  if (!dateStr) return ""
  const d = new Date(dateStr)
  const pad = (n: number) => (n < 10 ? "0" + n : String(n))
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function loadHistoryItem(h: AiHistoryItem) {
  try {
    const parsed = JSON.parse(h.resultJson)
    if (Array.isArray(parsed) && parsed.length > 0) {
      currentView.value = "chat"
      messages.value.push({ role: "ai", recipes: parsed, expanded: -1 })
      scrollToBottom()
    }
  } catch {
    // 历史数据解析失败时忽略
  }
}
</script>

<style scoped>
.ai-page {
  padding: 24px 0 40px;
}

.ai-shell {
  max-width: 900px;
  margin: 0 auto;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 112px);
  min-height: 520px;
}

.ai-tabs {
  display: flex;
  border-bottom: 1px solid var(--line);
  background: #fffdf8;
  flex-shrink: 0;
}

.ai-tab {
  flex: 1;
  border: 0;
  background: none;
  padding: 15px 0;
  font-size: 15px;
  font-weight: 600;
  color: #7d857f;
  position: relative;
}

.ai-tab.active {
  color: var(--green);
}

.ai-tab.active::after {
  content: "";
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 44px;
  height: 3px;
  border-radius: 2px;
  background: var(--green);
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.chat-welcome {
  text-align: center;
  padding: 40px 20px;
}

.welcome-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--leaf), var(--green));
  color: #fff;
  font-weight: 800;
  box-shadow: 0 10px 24px rgba(47, 93, 58, 0.25);
}

.chat-welcome h2 {
  margin: 0 0 8px;
  font-size: 22px;
  color: var(--green-dark);
}

.chat-welcome p {
  margin: 0 0 18px;
  color: var(--muted);
  font-size: 14px;
}

.welcome-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.chip {
  border: 1px solid var(--line);
  background: #f4efe4;
  border-radius: 999px;
  padding: 7px 16px;
  font-size: 13px;
  color: var(--green);
  transition: background 0.15s ease;
}

.chip:hover {
  background: #e7dfcd;
}

.msg {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.msg.user {
  justify-content: flex-end;
}

.ai-avatar {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--leaf), var(--green));
  color: #fff;
  font-size: 12px;
  font-weight: 800;
}

.msg-body {
  max-width: 78%;
  min-width: 0;
}

.msg.user .msg-body {
  max-width: 64%;
}

.bubble {
  border-radius: 16px;
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.user .bubble {
  background: var(--green);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.ai-bubble {
  background: #f4efe4;
  color: var(--ink);
  border-bottom-left-radius: 4px;
}

.thinking {
  color: var(--muted);
  font-size: 13px;
  padding: 4px 2px;
}

.recipe-cards {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.recipe-card {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 14px;
  cursor: pointer;
  transition: box-shadow 0.15s ease;
}

.recipe-card:hover {
  box-shadow: var(--shadow);
}

.rc-top {
  display: flex;
  gap: 12px;
  align-items: center;
}

.rc-cover {
  width: 84px;
  height: 84px;
  flex-shrink: 0;
  border-radius: 10px;
  overflow: hidden;
  background: #eee8dc;
}

.rc-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.rc-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  font-weight: 800;
  color: #fff;
  background: linear-gradient(135deg, var(--leaf), var(--green));
}

.rc-head {
  flex: 1;
  min-width: 0;
}

.rc-name-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.rc-name-row h3 {
  margin: 0;
  font-size: 16px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rc-arrow {
  font-size: 12px;
  color: var(--tomato);
  flex-shrink: 0;
}

.rc-meta {
  display: flex;
  gap: 6px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.rc-detail {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0ead9;
}

.rc-section {
  margin-bottom: 12px;
}

.rc-section h4 {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--muted);
}

.rc-ing {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 5px 0;
  border-bottom: 1px solid #f7f2e6;
  font-size: 13px;
}

.rc-ing span:last-child {
  color: var(--muted);
}

.rc-step {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  line-height: 1.6;
}

.rc-step-no {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--tomato);
  color: #fff;
  font-size: 11px;
  font-weight: 800;
}

.input-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
  border-top: 1px solid var(--line);
  background: #fffdf8;
  flex-shrink: 0;
}

.chat-input {
  flex: 1;
  min-width: 0;
  height: 42px;
  border: 1px solid var(--line);
  border-radius: 999px;
  outline: 0;
  background: #f4efe4;
  padding: 0 18px;
  font-size: 14px;
}

.chat-input:focus {
  border-color: var(--leaf);
  background: #fff;
}

.send-btn {
  flex-shrink: 0;
}

.history-area {
  flex: 1;
  overflow-y: auto;
  padding: 18px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.history-item {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 14px 16px;
  cursor: pointer;
  transition: box-shadow 0.15s ease;
}

.history-item:hover {
  box-shadow: var(--shadow);
}

.hi-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.hi-time {
  flex: 1;
  font-size: 12px;
  color: #a6ada7;
}

.hi-rating {
  font-size: 12px;
  color: var(--amber);
  font-weight: 700;
}

.hi-input {
  margin: 0;
  font-size: 14px;
  color: #5c655e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 640px) {
  .ai-page {
    padding: 12px 0 92px;
  }

  .ai-shell {
    height: calc(100vh - 150px);
    border-radius: 12px;
    min-height: 420px;
  }

  .msg-body,
  .msg.user .msg-body {
    max-width: 86%;
  }

  .messages {
    padding: 14px;
  }
}
</style>
