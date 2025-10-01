import React from 'react'

export default function OAuthTest() {
  const handleOAuthClick = () => {
    console.log('=== OAuth Test Button Clicked ===')
    console.log('Current URL:', window.location.href)
    console.log('Target URL:', '/oauth2/authorization/google')
    console.log('About to navigate...')

    try {
      window.location.href = '/oauth2/authorization/google'
      console.log('Navigation command executed')
    } catch (error) {
      console.error('Navigation failed:', error)
    }
  }

  const handleDirectNavigate = () => {
    console.log('=== Direct Navigation Test ===')
    window.location.href = 'http://localhost:5173/oauth2/authorization/google'
  }

  const handleBackendDirect = () => {
    console.log('=== Backend Direct Test ===')
    window.location.href = 'http://localhost:8080/oauth2/authorization/google'
  }

  return (
    <div style={{ padding: '20px', fontFamily: 'Arial, sans-serif' }}>
      <h1>OAuth Diagnostic Test Page</h1>

      <div style={{ marginBottom: '20px' }}>
        <h2>Current Environment:</h2>
        <p>Current URL: {window.location.href}</p>
        <p>Origin: {window.location.origin}</p>
      </div>

      <div style={{ marginBottom: '20px' }}>
        <h2>OAuth Button Tests:</h2>

        <button
          onClick={handleOAuthClick}
          style={{
            padding: '10px 20px',
            margin: '5px',
            backgroundColor: '#4285f4',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          Test OAuth (Relative URL)
        </button>

        <button
          onClick={handleDirectNavigate}
          style={{
            padding: '10px 20px',
            margin: '5px',
            backgroundColor: '#34a853',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          Test Direct Frontend
        </button>

        <button
          onClick={handleBackendDirect}
          style={{
            padding: '10px 20px',
            margin: '5px',
            backgroundColor: '#ea4335',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          Test Direct Backend
        </button>
      </div>

      <div style={{ marginBottom: '20px' }}>
        <h2>Direct Links:</h2>
        <div>
          <a href="/oauth2/authorization/google" target="_blank" style={{ color: '#4285f4', textDecoration: 'none', marginRight: '20px' }}>
            Relative OAuth Link
          </a>
          <a href="http://localhost:5173/oauth2/authorization/google" target="_blank" style={{ color: '#34a853', textDecoration: 'none', marginRight: '20px' }}>
            Frontend OAuth Link
          </a>
          <a href="http://localhost:8080/oauth2/authorization/google" target="_blank" style={{ color: '#ea4335', textDecoration: 'none' }}>
            Backend OAuth Link
          </a>
        </div>
      </div>

      <div>
        <h2>Debug Info:</h2>
        <button
          onClick={() => {
            console.log('=== Debug Info ===')
            console.log('localStorage keys:', Object.keys(localStorage))
            console.log('document.cookie:', document.cookie)
            console.log('User agent:', navigator.userAgent)
          }}
          style={{
            padding: '10px 20px',
            backgroundColor: '#f4b400',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          Log Debug Info
        </button>
      </div>
    </div>
  )
}