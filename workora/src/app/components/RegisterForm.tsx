'use client';
import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Eye, EyeOff, Loader2, CheckCircle2, XCircle } from 'lucide-react';
import { toast } from 'sonner';
import { register as registerUser } from '@/lib/auth';
import { AxiosError } from 'axios';

interface RegisterFormData {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  confirmPassword: string;
  terms: boolean;
}

interface Props {
  onSuccess: (email: string, verificationSessionToken?: string) => void;
  onSwitchToLogin: () => void;
}

const passwordChecks = [
  { label: 'At least 8 characters', test: (p: string) => p.length >= 8 },
  { label: 'One uppercase letter', test: (p: string) => /[A-Z]/.test(p) },
  { label: 'One number', test: (p: string) => /\d/.test(p) },
  { label: 'One special character', test: (p: string) => /[@$!%*?&#^]/.test(p) },
];

export default function RegisterForm({ onSuccess, onSwitchToLogin }: Props) {
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormData>({
    defaultValues: { firstName: '', lastName: '', email: '', password: '', confirmPassword: '', terms: false },
  });

  const watchedPassword = watch('password', '');

  const onSubmit = async (data: RegisterFormData) => {
    try {
      const result = await registerUser({
        firstName: data.firstName,
        lastName: data.lastName,
        email: data.email,
        password: data.password,
      });
      toast.success('Account created! Check your email to verify.');
      onSuccess(data.email, result.verificationSessionToken);
    } catch (err: unknown) {
      let message = 'Registration failed. Please try again.';
      if (err instanceof AxiosError) {
        const apiMsg = err.response?.data?.message;
        if (apiMsg) message = apiMsg;
        else if (err.response?.status === 409) message = 'An account with this email already exists.';
        else if (err.response?.status === 422) message = 'Invalid registration data. Please check your inputs.';
        else if (!err.response) message = 'Cannot reach the server. Make sure the backend is running on localhost:8080.';
      } else if (err instanceof Error) {
        message = err.message;
      }
      toast.error(message);
    }
  };

  const passedChecks = passwordChecks.filter(c => c.test(watchedPassword)).length;
  const strengthPercent = (passedChecks / passwordChecks.length) * 100;
  const strengthColor =
    passedChecks === 0 ? 'bg-border' :
    passedChecks <= 1 ? 'bg-red-500' :
    passedChecks <= 2 ? 'bg-amber-500' :
    passedChecks <= 3 ? 'bg-yellow-400' : 'bg-green-500';

  return (
    <div className="fade-in">
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-foreground">Create your account</h2>
        <p className="text-muted-foreground text-sm mt-1">Join thousands of teams using Workora</p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {/* Name row */}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5" htmlFor="reg-first">
              First name
            </label>
            <input
              id="reg-first"
              type="text"
              autoComplete="given-name"
              placeholder="Sophea"
              className={`input-base ${errors.firstName ? 'input-error' : ''}`}
              {...register('firstName', { required: 'Required' })}
            />
            {errors.firstName && <p className="mt-1 text-xs text-red-500">{errors.firstName.message}</p>}
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5" htmlFor="reg-last">
              Last name
            </label>
            <input
              id="reg-last"
              type="text"
              autoComplete="family-name"
              placeholder="Chan"
              className={`input-base ${errors.lastName ? 'input-error' : ''}`}
              {...register('lastName', { required: 'Required' })}
            />
            {errors.lastName && <p className="mt-1 text-xs text-red-500">{errors.lastName.message}</p>}
          </div>
        </div>

        {/* Email */}
        <div>
          <label className="block text-sm font-medium text-foreground mb-1.5" htmlFor="reg-email">
            Work email
          </label>
          <input
            id="reg-email"
            type="email"
            autoComplete="email"
            placeholder="you@company.com"
            className={`input-base ${errors.email ? 'input-error' : ''}`}
            {...register('email', {
              required: 'Email is required',
              pattern: { value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: 'Enter a valid email' },
            })}
          />
          {errors.email && <p className="mt-1 text-xs text-red-500">{errors.email.message}</p>}
        </div>

        {/* Password */}
        <div>
          <label className="block text-sm font-medium text-foreground mb-1.5" htmlFor="reg-password">
            Password
          </label>
          <div className="relative">
            <input
              id="reg-password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="new-password"
              placeholder="Create a strong password"
              className={`input-base pr-10 ${errors.password ? 'input-error' : ''}`}
              {...register('password', {
                required: 'Password is required',
                minLength: { value: 8, message: 'Minimum 8 characters' },
              })}
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
            >
              {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          </div>
          {errors.password && <p className="mt-1 text-xs text-red-500">{errors.password.message}</p>}

          {/* Strength meter */}
          {watchedPassword.length > 0 && (
            <div className="mt-2">
              <div className="progress-bar mb-2">
                <div
                  className={`progress-fill ${strengthColor}`}
                  style={{ width: `${strengthPercent}%` }}
                />
              </div>
              <div className="grid grid-cols-2 gap-1">
                {passwordChecks.map((check, i) => {
                  const passed = check.test(watchedPassword);
                  return (
                    <div key={`pwcheck-${i}`} className="flex items-center gap-1">
                      {passed
                        ? <CheckCircle2 size={11} className="text-green-500 flex-shrink-0" />
                        : <XCircle size={11} className="text-muted-foreground flex-shrink-0" />}
                      <span className={`text-xs ${passed ? 'text-green-600' : 'text-muted-foreground'}`}>
                        {check.label}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>

        {/* Confirm password */}
        <div>
          <label className="block text-sm font-medium text-foreground mb-1.5" htmlFor="reg-confirm">
            Confirm password
          </label>
          <div className="relative">
            <input
              id="reg-confirm"
              type={showConfirm ? 'text' : 'password'}
              autoComplete="new-password"
              placeholder="Repeat your password"
              className={`input-base pr-10 ${errors.confirmPassword ? 'input-error' : ''}`}
              {...register('confirmPassword', {
                required: 'Please confirm your password',
                validate: (v) => v === watchedPassword || 'Passwords do not match',
              })}
            />
            <button
              type="button"
              onClick={() => setShowConfirm(!showConfirm)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
            >
              {showConfirm ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          </div>
          {errors.confirmPassword && <p className="mt-1 text-xs text-red-500">{errors.confirmPassword.message}</p>}
        </div>

        {/* Terms */}
        <div>
          <div className="flex items-start gap-2">
            <input
              id="reg-terms"
              type="checkbox"
              className="w-4 h-4 mt-0.5 rounded border-input text-primary"
              {...register('terms', { required: 'You must accept the terms to continue' })}
            />
            <label htmlFor="reg-terms" className="text-sm text-muted-foreground leading-relaxed">
              I agree to Workora&apos;s{' '}
              <a href="#" className="text-primary hover:underline">Terms of Service</a>
              {' '}and{' '}
              <a href="#" className="text-primary hover:underline">Privacy Policy</a>
            </label>
          </div>
          {errors.terms && <p className="mt-1 text-xs text-red-500">{errors.terms.message}</p>}
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="btn-primary w-full py-2.5 rounded-lg font-semibold text-sm flex items-center justify-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed"
          style={{ minHeight: '42px' }}
        >
          {isSubmitting ? (
            <>
              <Loader2 size={16} className="animate-spin" />
              <span>Creating account…</span>
            </>
          ) : (
            'Create Account'
          )}
        </button>
      </form>

      <p className="text-center text-sm text-muted-foreground mt-5">
        Already have an account?{' '}
        <button onClick={onSwitchToLogin} className="text-primary font-semibold hover:underline">
          Sign in
        </button>
      </p>
    </div>
  );
}