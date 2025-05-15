<template>
  <div class="settings-view">
    <div class="settings-header">
      <h1>系统设置</h1>
    </div>

    <div class="settings-container">
      <div v-if="loading" class="loading">
        <div class="loading-spinner"></div>
        <p>正在加载配置...</p>
      </div>

      <div v-if="error" class="error">{{ error }}</div>

      <form v-if="!loading && !error && config" @submit.prevent="saveConfig" class="settings-form">
        <div class="form-group">
          <label for="blogStoragePath">博客存储路径</label>
          <input type="text" id="blogStoragePath" v-model="config.blogStoragePath" />
        </div>

        <div class="form-group">
          <label for="imageStoragePath">图片存储路径</label>
          <input type="text" id="imageStoragePath" v-model="config.imageStoragePath" />
        </div>

        <div class="form-group">
          <label for="xmodelAPIKey">XModel API Key</label>
          <input type="text" id="xmodelAPIKey" v-model="config.xmodelAPIKey" />
        </div>

        <div class="form-group">
          <label for="bigmodelAPIKey">BigModel API Key</label>
          <input type="text" id="bigmodelAPIKey" v-model="config.bigmodelAPIKey" />
        </div>

        <button type="submit" :disabled="saving" class="save-button">
          {{ saving ? '保存中...' : '保存设置' }}
        </button>

        <div v-if="saveSuccess" class="success-message">
          <span class="message-icon">✓</span> 设置已成功保存！
        </div>

        <div v-if="saveError" class="error-message">
          <span class="message-icon">✗</span> {{ saveError }}
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

interface SystemConfig {
  blogStoragePath: string
  imageStoragePath: string
  xmodelAPIKey: string
  bigmodelAPIKey: string
}

const config = ref<SystemConfig | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const saving = ref(false)
const saveSuccess = ref(false)
const saveError = ref<string | null>(null)

const fetchConfig = async () => {
  loading.value = true
  error.value = null
  try {
    const response = await fetch('/config/get')
    if (!response.ok) {
      throw new Error('获取配置失败')
    }
    config.value = await response.json()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载配置时发生未知错误'
    console.error(err)
  } finally {
    loading.value = false
  }
}

const saveConfig = async () => {
  if (!config.value) return
  saving.value = true
  saveSuccess.value = false
  saveError.value = null
  try {
    const response = await fetch('/config/set', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(config.value),
    })
    const result = await response.json()
    if (response.ok && result === true) {
      saveSuccess.value = true
      // 3秒后自动隐藏成功消息
      setTimeout(() => {
        saveSuccess.value = false
      }, 3000)
    } else {
      throw new Error(result.message || '保存配置失败')
    }
  } catch (err) {
    saveError.value = err instanceof Error ? err.message : '保存配置时发生未知错误'
    console.error(err)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchConfig()
})
</script>

<style scoped>
.settings-view {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.settings-header {
  width: 100%;
  text-align: center;
  padding: 2rem 0;
}

.settings-header h1 {
  color: var(--color-heading);
  font-size: 2rem;
  margin: 0;
}

.settings-container {
  width: 100%;
  max-width: 600px;
  background-color: var(--color-card-bg);
  border-radius: var(--border-radius);
  box-shadow: var(--box-shadow);
  padding: 2rem;
  margin-bottom: 2rem;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem;
}

.loading-spinner {
  border: 4px solid rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  border-top: 4px solid var(--color-primary);
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.error {
  color: var(--color-error);
  text-align: center;
  padding: 1rem;
  background-color: rgba(244, 67, 54, 0.1);
  border-radius: var(--border-radius);
  border-left: 3px solid var(--color-error);
  margin: 1rem 0;
}

.settings-form {
  display: flex;
  flex-direction: column;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  color: var(--color-heading);
  font-weight: 500;
}

.form-group input[type='text'] {
  width: 100%;
  padding: 0.8rem;
  background-color: rgba(255, 255, 255, 0.05);
  color: var(--color-text);
  border: 1px solid var(--color-border);
  border-radius: var(--border-radius);
  font-size: 1rem;
  transition:
    border-color 0.3s,
    box-shadow 0.3s;
  box-sizing: border-box;
}

.form-group input[type='text']:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(74, 136, 229, 0.2);
}

.save-button {
  background-color: var(--color-primary);
  color: white;
  padding: 0.8rem 1.5rem;
  border: none;
  border-radius: var(--border-radius);
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
  transition: background-color 0.3s;
  margin-top: 1rem;
  align-self: flex-end;
}

.save-button:hover:not(:disabled) {
  background-color: var(--color-primary-dark);
}

.save-button:disabled {
  background-color: var(--color-border);
  cursor: not-allowed;
  opacity: 0.7;
}

.success-message,
.error-message {
  display: flex;
  align-items: center;
  margin-top: 1.5rem;
  padding: 1rem;
  border-radius: var(--border-radius);
  font-weight: 500;
}

.success-message {
  background-color: rgba(76, 175, 80, 0.1);
  color: #4caf50;
  border-left: 3px solid #4caf50;
}

.error-message {
  background-color: rgba(244, 67, 54, 0.1);
  color: #f44336;
  border-left: 3px solid #f44336;
}

.message-icon {
  font-size: 1.2rem;
  margin-right: 0.5rem;
}

@media (max-width: 768px) {
  .settings-container {
    padding: 1.5rem;
  }

  .settings-header h1 {
    font-size: 1.5rem;
  }

  .save-button {
    width: 100%;
  }
}
</style>
