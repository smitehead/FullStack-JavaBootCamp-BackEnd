import axios from 'axios';

export const api = axios.create({
  baseURL: 'http://localhost:8080/api', // Spring Boot 백엔드 URL
  withCredentials: true,
});

api.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem('java_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 401 응답 인터셉터: 다른 기기에서 로그인으로 토큰이 무효화된 경우 자동 로그아웃
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // /auth/logout 요청 자체는 제외 (무한루프 방지)
      const url = error.config?.url ?? '';
      if (!url.includes('/auth/logout')) {
        // AppContext의 커스텀 이벤트 리스너로 모달 표시 위임
        window.dispatchEvent(new CustomEvent('forceLogout'));
      }
    }
    return Promise.reject(error);
  }
);

export default api;
