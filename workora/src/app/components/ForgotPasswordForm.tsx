'use client';
import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Loader2, ArrowLeft, Mail, KeyRound, Eye, EyeOff, CheckCircle } from 'lucide-react';
import { toast } from 'sonner';
import { requestPasswordReset, confirmPasswordReset } from '@/lib/auth';
import { AxiosError } from 'axios';

type Step = 'request' | 'confirm' | 'done';

interface RequestFormData {
  email: string;
}

interface ConfirmFormData {
  token: string;
  newPassword: string;
  confirmPassword: string;
}

interface Props {
  onBack: () => void;
  initialEmail?: string;
}

export default function ForgotPasswordForm({ onBack, initialEmail = '' }: Props) {
  const [step, setStep] = useState<Step>('request');
  const [submittedEmail, setSubmittedEmail] = useState(initialEmail);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const requestForm = useForm<RequestFormData>({
    defaultValues: { email: initialEmail },
  });

  const confirmForm = useForm<ConfirmFormData>({
    defaultValues: { token: '', newPassword: '', confirmPassword: '' },
  });

  const handleRequest = async (data: RequestFormData) => {
    try {
      await requestPasswordReset(data.email);
      setSubmittedEmail(data.email);
      setStep('confirm');
      toast.success('If this email exists, a reset link has been sent.');
    } catch (err: unknown) {
      let message = 'Failed to send reset email. Please try again.';
      if (err instanceof AxiosError) {
        const apiMsg = err.response?.data?.message;
        if (apiMsg) message = apiMsg;
        else if (!err.response) message = 'Cannot reach the server. Make sure the backend is running on localhost:8080.';
      }
      toast.error(message);
    }
  };

  const handleConfirm = async (data: ConfirmFormData) => {
    try {
      await confirmPasswordReset({
        email: submittedEmail,
        token: data.token,
        newPassword: data.newPassword,
      });
      setStep('done');
      toast.success('Password reset successfully!');
    } catch (err: unknown) {
      let message = 'Password reset failed. Please check your token and try again.';
      if (err instanceof AxiosError) {
        const apiMsg = err.response?.data?.message;
        if (apiMsg) message = apiMsg;
        else if (err.response?.status === 400) message = 'Invalid or expired reset token.';
        else if (!err.response) message = 'Cannot reach the server. Make sure the backend is running on localhost:8080.';
      }
      toast.error(message);
    }
  };

  if (step === 'done') {
    return (
      <div className="fade-scale-in text-center">
        <div className="w-16 h-16 bg-green-50 rounded-2xl flex items-center justify-center mx-auto mb-6">
          <CheckCircle size={28} className="text-green-500" />
        </div>
        <h2 className="text-2xl font-bold text-foreground mb-2">Password reset!</h2>
        <p className="text-muted-foreground text-sm mb-6">
          Your password has been updated. You can now sign in with your new password.
        </p>
        <button
          onClick={onBack}
          className="btn-primary w-full py-2.5 rounded-lg font-semibold text-sm"
        >
          Back to Sign In
        </button>
      </div>
    );
  }

  if (step === 'confirm') {
    return (
      <div className="fade-in">
        <div className="mb-6">
          <h2 className="text-2xl font-bold text-foreground">Reset your password</h2>
          <p className="text-muted-foreground text-sm mt-1">
            Enter the reset token from your email and choose a new password.
          </p>
        </div>

        <div className="mb-4 p-3 rounded-lg bg-muted/50 border border-border">
          <p className="text-xs text-muted-foreground">
            Reset email sent to: <span className="font-medium text-foreground">{submittedEmail}</span>
          </p>
        </div>

        <form onSubmit={confirmForm.handleSubmit(handleConfirm)} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5" htmlFor="reset-token">
              Reset token
            </label>
            <input
              id="reset-token"
              type="text"
              placeholder="Enter token from email"
              className={`input-base font-mono ${confirmForm.formState.errors.token ? 'input-error' : ''}`}
              {...confirmForm.register('token', { required: 'Reset token is required' })}
            />
            {confirmForm.formState.errors.token && (
              <p className="mt-1 text-xs text-red-500">{confirmForm.formState.errors.token.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5" htmlFor="new-password">
              New password
            </label>
            <div className="relative">
              <input
                id="new-password"
                type={showPassword ? 'text' : 'password'}
                placeholder="Create a new password"
                className={`input-base pr-10 ${confirmForm.formState.errors.newPassword ? 'input-error' : ''}`}
                {...confirmForm.register('newPassword', {
                  required: 'New password is required',
                  minLength: { value: 8, message: 'Minimum 8 characters' },
                  pattern: {
                    value: /^(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#^])/,
                    message: 'Must include uppercase, number, and special character',
                  },
                })}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
              >
                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
            {confirmForm.formState.errors.newPassword && (
              <p className="mt-1 text-xs text-red-500">{confirmForm.formState.errors.newPassword.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5" htmlFor="confirm-new-password">
              Confirm new password
            </label>
            <div className="relative">
              <input
                id="confirm-new-password"
                type={showConfirm ? 'text' : 'password'}
                placeholder="Repeat your new password"
                className={`input-base pr-10 ${confirmForm.formState.errors.confirmPassword ? 'input-error' : ''}`}
                {...confirmForm.register('confirmPassword', {
                  required: 'Please confirm your password',
                  validate: (v) =>
                    v === confirmForm.watch('newPassword') || 'Passwords do not match',
                })}
              />
              <button
                type="button"
                onClick={() => setShowConfirm(!showConfirm)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
              >
                {showConfirm ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
            {confirmForm.formState.errors.confirmPassword && (
              <p className="mt-1 text-xs text-red-500">{confirmForm.formState.errors.confirmPassword.message}</p>
            )}
          </div>

          <button
            type="submit"
            disabled={confirmForm.formState.isSubmitting}
            className="btn-primary w-full py-2.5 rounded-lg font-semibold text-sm flex items-center justify-center gap-2 disabled:opacity-60"
            style={{ minHeight: '42px' }}
          >
            {confirmForm.formState.isSubmitting ? (
              <><Loader2 size={16} className="animate-spin" /><span>Resetting…</span></>
            ) : (
              <><KeyRound size={16} /><span>Reset Password</span></>
            )}
          </button>
        </form>

        <button
          onClick={() => setStep('request')}
          className="flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors mx-auto mt-4"
        >
          <ArrowLeft size={14} />
          Back
        </button>
      </div>
    );
  }

  return (
    <div className="fade-in">
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-foreground">Forgot password?</h2>
        <p className="text-muted-foreground text-sm mt-1">
          Enter your email and we&apos;ll send you a reset link.
        </p>
      </div>

      <form onSubmit={requestForm.handleSubmit(handleRequest)} className="space-y-5">
        <div>
          <label className="block text-sm font-medium text-foreground mb-1.5" htmlFor="forgot-email">
            Email address
          </label>
          <div className="relative">
            <Mail size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
            <input
              id="forgot-email"
              type="email"
              autoComplete="email"
              placeholder="you@company.com"
              className={`input-base pl-9 ${requestForm.formState.errors.email ? 'input-error' : ''}`}
              {...requestForm.register('email', {
                required: 'Email address is required',
                pattern: { value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: 'Enter a valid email address' },
              })}
            />
          </div>
          {requestForm.formState.errors.email && (
            <p className="mt-1 text-xs text-red-500">{requestForm.formState.errors.email.message}</p>
          )}
        </div>

        <button
          type="submit"
          disabled={requestForm.formState.isSubmitting}
          className="btn-primary w-full py-2.5 rounded-lg font-semibold text-sm flex items-center justify-center gap-2 disabled:opacity-60"
          style={{ minHeight: '42px' }}
        >
          {requestForm.formState.isSubmitting ? (
            <><Loader2 size={16} className="animate-spin" /><span>Sending…</span></>
          ) : (
            'Send Reset Link'
          )}
        </button>
      </form>

      <button
        onClick={onBack}
        className="flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors mx-auto mt-5"
      >
        <ArrowLeft size={14} />
        Back to sign in
      </button>
    </div>
  );
}
