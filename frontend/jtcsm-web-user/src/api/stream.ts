import { getToken } from "@/utils/token"
import type { AiGenerateParams } from "@/api"

export interface StreamEventHandlers {
  onDelta?: (content: string) => void
  onDone?: (data: any) => void
  onError?: (message: string) => void
}

export interface AbortHandle {
  abort: () => void
}

function parseEventBlock(block: string, handlers: StreamEventHandlers) {
  let eventName = "message"
  const dataLines: string[] = []
  for (const line of block.split("\n")) {
    if (line.startsWith("event:")) {
      eventName = line.slice(6).trim()
    } else if (line.startsWith("data:")) {
      dataLines.push(line.slice(5).replace(/^ /, ""))
    }
  }
  if (dataLines.length === 0) return

  let payload: any
  try {
    payload = JSON.parse(dataLines.join("\n"))
  } catch {
    return
  }

  if (eventName === "delta" && typeof payload.content === "string") {
    handlers.onDelta?.(payload.content)
  } else if (eventName === "done") {
    handlers.onDone?.(payload)
  } else if (eventName === "error") {
    handlers.onError?.(payload.message || "AI 生成失败")
  }
}

export function aiGenerateStream(
  params: AiGenerateParams,
  handlers: StreamEventHandlers
): AbortHandle {
  const controller = new AbortController()

  ;(async () => {
    try {
      const response = await fetch("/api/v1/ai/generate/stream", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${getToken()}`
        },
        body: JSON.stringify(params),
        signal: controller.signal
      })

      if (!response.ok) {
        handlers.onError?.(`请求失败：${response.status}`)
        return
      }
      if (!response.body) {
        handlers.onError?.("浏览器不支持流式响应")
        return
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder("utf-8")
      let buffer = ""

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const parts = buffer.split("\n\n")
        buffer = parts.pop() || ""
        for (const part of parts) {
          if (part.trim()) parseEventBlock(part.trimEnd(), handlers)
        }
      }
      if (buffer.trim()) parseEventBlock(buffer.trim(), handlers)
    } catch (err: any) {
      if (err?.name === "AbortError") return
      handlers.onError?.("网络异常，请重试")
    }
  })()

  return {
    abort: () => controller.abort()
  }
}
