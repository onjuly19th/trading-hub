const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

// 토큰 가져오기
const getToken = () => {
  if (typeof window !== 'undefined') {
    return localStorage.getItem('token');
  }
  return null;
};

// 기본 fetch 설정
const fetchWithConfig = async (url, options = {}) => {
  const token = getToken();
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const response = await fetch(`${API_BASE_URL}${url}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const error = new Error('API request failed');
    error.status = response.status;
    try {
      const responseText = await response.text();
      try {
        const errorData = JSON.parse(responseText);
        console.error('Server error details:', {
          status: response.status,
          statusText: response.statusText,
          data: errorData
        });
        error.data = errorData;
      } catch {
        console.error('Server error (non-JSON):', {
          status: response.status,
          statusText: response.statusText,
          text: responseText
        });
        error.data = { message: responseText };
      }
    } catch (e) {
      console.error('Failed to read error response:', e);
      error.data = { message: `Server error (${response.status}: ${response.statusText})` };
    }
    throw error;
  }

  const responseText = await response.text();
  if (!responseText) {
    return null;
  }
  try {
    return JSON.parse(responseText);
  } catch {
    return responseText;
  }
};

// GET 요청
export const get = (url) => fetchWithConfig(url);

// POST 요청
export const post = (url, data) => fetchWithConfig(url, {
  method: 'POST',
  body: JSON.stringify(data),
});

// PUT 요청
export const put = (url, data) => fetchWithConfig(url, {
  method: 'PUT',
  body: JSON.stringify(data),
});

// DELETE 요청
export const del = (url) => fetchWithConfig(url, {
  method: 'DELETE',
}); 