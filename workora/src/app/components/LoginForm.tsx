'use client';
import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useRouter } from 'next/navigation';
import { Eye, EyeOff, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { login, storeTokens, storeUser } from '@/lib/auth';
import { AxiosError } from 'axios';

interface LoginFormData {
  email: string;
  password: string;
  rememberMe: boolean;
}

interface Props {
  onSwitchToRegister: () => void;
  onForgotPassword?: () => void;
}

export default function LoginForm({ onSwitchToRegister, onForgotPassword }: Props) {
  const router = useRouter();
  const [showPassword, setShowPassword] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({
    defaultValues: { email: '', password: '', rememberMe: false },
  });

  const onSubmit = async (data: LoginFormData) => {
    try {
      const { tokens, user } = await login(data.email, data.password);
      storeTokens(tokens);
      storeUser(user);
      toast.success(`Welcome back, ${user.firstName}!`);
      router.push('/dashboard');
    } catch (err: unknown) {
      let message = 'Login failed. Please check your credentials and try again.';
      if (err instanceof AxiosError) {
        const apiMsg = err.response?.data?.message;
        if (apiMsg) message = apiMsg;
        else if (err.response?.status === 401) message = 'Invalid email or password.';
        else if (err.response?.status === 403) message = 'Your account is not verified or has been disabled.';
        else if (!err.response) message = 'Cannot reach the server. Make sure the backend is running on localhost:8080.';
      } else if (err instanceof Error) {
        message = err.message;
      }
      toast.error(message);
    }
  };

  return (
    <div className="fade-in">
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-foreground">Welcome back</h2>
        <p className="text-muted-foreground text-sm mt-1">Sign in to your Workora account</p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
        {/* Email */}
        <div>
          <label className="block text-sm font-medium text-foreground mb-1.5" htmlFor="login-email">
            Email address
          </label>
          <input
            id="login-email"
            type="email"
            autoComplete="email"
            placeholder="you@company.com"
            className={`input-base ${errors.email ? 'input-error' : ''}`}
            {...register('email', {
              required: 'Email address is required',
              pattern: { value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: 'Enter a valid email address' },
            })}
          />
          {errors.email && (
            <p className="mt-1 text-xs text-red-500 flex items-center gap-1">{errors.email.message}</p>
          )}
        </div>

        {/* Password */}
        <div>
          <div className="flex items-center justify-between mb-1.5">
            <label className="block text-sm font-medium text-foreground" htmlFor="login-password">
              Password
            </label>
            <button
              type="button"
              onClick={onForgotPassword}
              className="text-xs text-primary hover:underline font-medium"
            >
              Forgot password?
            </button>
          </div>
          <div className="relative">
            <input
              id="login-password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
              placeholder="Enter your password"
              className={`input-base pr-10 ${errors.password ? 'input-error' : ''}`}
              {...register('password', {
                required: 'Password is required',
                minLength: { value: 6, message: 'Password must be at least 6 characters' },
              })}
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
              aria-label={showPassword ? 'Hide password' : 'Show password'}
            >
              {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          </div>
          {errors.password && (
            <p className="mt-1 text-xs text-red-500">{errors.password.message}</p>
          )}
        </div>

        {/* Remember me */}
        <div className="flex items-center gap-2">
          <input
            id="remember-me"
            type="checkbox"
            className="w-4 h-4 rounded border-input text-primary focus:ring-ring"
            {...register('rememberMe')}
          />
          <label htmlFor="remember-me" className="text-sm text-muted-foreground">
            Keep me signed in for 30 days
          </label>
        </div>

        {/* Submit */}
        <button
          type="submit"
          disabled={isSubmitting}
          className="btn-primary w-full py-2.5 rounded-lg font-semibold text-sm flex items-center justify-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed"
          style={{ minHeight: '42px' }}
        >
          {isSubmitting ? (
            <>
              <Loader2 size={16} className="animate-spin" />
              <span>Signing in…</span>
            </>
          ) : (
            'Sign In'
          )}
        </button>
      </form>

      {/* API info banner */}
      <div className="mt-5 p-3 rounded-lg border border-border bg-muted/40">
        <p className="text-xs text-muted-foreground text-center">
          Connects to backend at{' '}
          <span className="font-mono text-foreground font-medium">
            {process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080'}
          </span>
        </p>
      </div>

      <p className="text-center text-sm text-muted-foreground mt-5">
        Don&apos;t have an account?{' '}
        <button onClick={onSwitchToRegister} className="text-primary font-semibold hover:underline">
          Create one free
        </button>
      </p>
    </div>
  );
}