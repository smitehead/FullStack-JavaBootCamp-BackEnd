import axios from 'axios';

export const api = axios.create({
  baseURL: 'http://localhost:8080/api', // Spring Boot 백엔드 URL
  withCredentials: true,
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('java_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export default api;
