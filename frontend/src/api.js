import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from './router.js'

const api = axios.create({ baseURL: '/api', timeout: 20000 })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers['X-Token'] = token
  return config
})

api.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) return body.data
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message))
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    const msg = error.response?.data?.message || error.message || '网络错误'
    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (router.currentRoute.value.path !== '/login') {
        ElMessage.error('登录已失效，请重新登录')
        router.push('/login')
      }
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

export default api

export function uploadFiles(files) {
  const fd = new FormData()
  for (const f of files) fd.append('files', f)
  return api.post('/files/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
}

export function photoList(csv) {
  if (!csv) return []
  return csv.split(',').map((s) => s.trim()).filter(Boolean)
}
