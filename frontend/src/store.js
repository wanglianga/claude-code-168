import { reactive } from 'vue'

export const store = reactive({
  user: JSON.parse(localStorage.getItem('user') || 'null'),
  currentFairDay: JSON.parse(localStorage.getItem('currentFairDay') || 'null'),

  setUser(user) {
    this.user = user
    if (user) localStorage.setItem('user', JSON.stringify(user))
    else localStorage.removeItem('user')
  },
  setFairDay(day) {
    this.currentFairDay = day
    if (day) localStorage.setItem('currentFairDay', JSON.stringify(day))
    else localStorage.removeItem('currentFairDay')
  },
  get fairDayId() {
    return this.currentFairDay?.id ?? null
  }
})
