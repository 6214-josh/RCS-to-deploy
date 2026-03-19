import { useState } from 'react';
import { useLocation } from 'wouter';

/**
 * Login Page - Industrial Dashboard
 * Design: Dark industrial theme with orange accent
 * Features: Simple credential-based authentication
 */
export default function Login() {
  const [, setLocation] = useLocation();
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin');
  const [error, setError] = useState('');

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    if (username === 'admin' && password === 'admin') {
      localStorage.setItem('isLoggedIn', 'true');
      setLocation('/');
    } else {
      setError('Invalid credentials');
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-[#1a1a1a]">
      <div className="w-full max-w-md p-8 bg-[#2d2d2d] border-t-4 border-[#f39c12] shadow-2xl rounded-lg">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-[#f39c12] tracking-widest uppercase">
            RCS System
          </h1>
          <p className="text-gray-400 mt-2">Logistics Management Portal</p>
        </div>

        <form onSubmit={handleLogin} className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-1">
              Username
            </label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full px-4 py-2 bg-[#1a1a1a] border border-[#3f3f3f] rounded focus:outline-none focus:border-[#f39c12] text-white"
              placeholder="Enter username"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-300 mb-1">
              Password
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full px-4 py-2 bg-[#1a1a1a] border border-[#3f3f3f] rounded focus:outline-none focus:border-[#f39c12] text-white"
              placeholder="Enter password"
            />
          </div>

          {error && <p className="text-red-400 text-sm">{error}</p>}

          <button
            type="submit"
            className="w-full py-3 bg-[#f39c12] hover:bg-yellow-600 text-[#1a1a1a] font-bold rounded transition duration-200 uppercase tracking-wider"
          >
            Access System
          </button>
        </form>

        <div className="mt-6 text-center text-xs text-gray-500">
          &copy; 2026 RCS Industrial Solutions
        </div>
      </div>
    </div>
  );
}
