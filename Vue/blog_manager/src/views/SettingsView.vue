<template>
  <div class="settings-view">
    <h1>系统设置</h1>
    <div v-if="loading" class="loading">正在加载配置...</div>
    <div v-if="error" class="error">{{ error }}</div>
    <form v-if="!loading && !error && config" @submit.prevent="saveConfig">
      <div class="form-group">
        <label for="blogStoragePath">博客存储路径:</label>
        <input type="text" id="blogStoragePath" v-model="config.blogStoragePath" />
      </div>
      <div class="form-group">
        <label for="imageStoragePath">图片存储路径:</label>
        <input type="text" id="imageStoragePath" v-model="config.imageStoragePath" />
      </div>
      <div class="form-group">
        <label for="xmodelAPIKey">XModel API Key:</label>
        <input type="text" id="xmodelAPIKey" v-model="config.xmodelAPIKey" />
      </div>
      <div class="form-group">
        <label for="bigmodelAPIKey">BigModel API Key:</label>
        <input type="text" id="bigmodelAPIKey" v-model="config.bigmodelAPIKey" />
      </div>
      <button type="submit" :disabled="saving">
        {{ saving ? '保存中...' : '保存设置' }}
      </button>
      <div v-if="saveSuccess" class="success-message">设置已成功保存！</div>
      <div v-if="saveError" class="error-message">{{ saveError }}</div>
    </form>
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
  max-width: 600px;
  margin: 20px auto;
  padding: 20px;
  background-color: #f9f9f9;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.loading,
.error {
  text-align: center;
  padding: 20px;
  font-size: 1.2em;
}

.error {
  color: red;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
}

.form-group input[type='text'] {
  width: calc(100% - 22px);
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
}

button {
  background-color: #4caf50;
  color: white;
  padding: 10px 15px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1em;
}

button:disabled {
  background-color: #ccc;
}

button:hover:not(:disabled) {
  background-color: #45a049;
}

.success-message {
  margin-top: 15px;
  color: green;
  font-weight: bold;
}

.error-message {
  margin-top: 15px;
  color: red;
  font-weight: bold;
}
</style>
