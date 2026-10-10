'use client';

import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { ArrowLeft, CheckCircle, Loader2, Mail } from 'lucide-react';
import { toast } from 'sonner';
import { requestPasswordReset } from '@/lib/auth';
import { AxiosError } from 'axios';

interface RequestFormData {
  email: string;
}

interface Props {
  onBack: () => void;
  initialEmail?: string;
}

export default function ForgotPasswordForm({ onBack, initialEmail = '' }: Props) {
  const [submittedEmail, setSubmittedEmail] = useState(initialEmail);
  const [emailSent, setEmailSent] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RequestFormData>({
    defaultValues: { email: initialEmail },
  });

  const handleRequest = async (data: RequestFormData) => {
    try {
      await requestPasswordReset(data.email);
      setSubmittedEmail(data.email);
      setEmailSent(true);
      toast.success('If this email exists, a password reset link has been sent.');
    } catch (err: unknown) {
      let message = 'Failed to send reset email. Please try again.';
      if (err instanceof AxiosError) {
        const apiMsg = err.response?.data?.message;
        if (apiMsg) message = apiMsg;
        else if (!err.response) {
          message = 'Cannot reach the server. Make sure the backend is running on localhost:8080.';
        }
      }
      toast.error(message);
    }
  };

  if (emailSent) {
    return (
      <div className="fade-scale-in text-center" role="status" aria-live="polite">
        <div className="w-16 h-16 bg-blue-50 rounded-2xl flex items-center justify-center mx-auto mb-6">
          <Mail size={28} className="text-primary" />
        </div>
        <h2 className="text-2xl font-bold text-foreground mb-2">Check Your Email</h2>
        <p className="text-muted-foreground text-sm leading-relaxed mb-3">
          If an account exists for this address, we&apos;ve sent a secure password reset link.
          Follow the instructions in that email to choose a new password.
        </p>
        <p className="font-medium text-foreground text-sm mb-6 bg-muted px-3 py-2 rounded-lg inline-block">
          {submittedEmail}
        </p>
        <div className="flex items-start gap-3 rounded-xl border border-border bg-muted/50 p-4 mb-6 text-left">
          <CheckCircle size={16} className="text-primary flex-shrink-0 mt-0.5" />
          <p className="text-xs text-muted-foreground leading-relaxed">
            The secure link opens Workora&apos;s password update page. If you don&apos;t see the
            email, check your spam or junk folder.
          </p>
        </div>
        <button
          onClick={onBack}
          className="btn-primary w-full py-2.5 rounded-lg font-semibold text-sm"
        >
          Back to Sign In
        </button>
      </div>
    );
  }

  return (
    <div className="fade-in">
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-foreground">Forgot password?</h2>
        <p className="text-muted-foreground text-sm mt-1">
          Enter your email and we&apos;ll send you a secure reset link.
        </p>
      </div>

      <form onSubmit={handleSubmit(handleRequest)} className="space-y-5">
        <div>
          <label
            className="block text-sm font-medium text-foreground mb-1.5"
            htmlFor="forgot-email"
          >
            Email address
          </label>
          <div className="relative">
            <Mail
              size={14}
              className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground"
              aria-hidden="true"
            />
            <input
              id="forgot-email"
              type="email"
              autoComplete="email"
              placeholder="you@company.com"
              className={`input-base pl-9 ${errors.email ? 'input-error' : ''}`}
              aria-invalid={Boolean(errors.email)}
              aria-describedby={errors.email ? 'forgot-email-error' : undefined}
              {...register('email', {
                required: 'Email address is required',
                pattern: {
                  value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                  message: 'Enter a valid email address',
                },
              })}
            />
          </div>
          {errors.email && (
            <p id="forgot-email-error" className="mt-1 text-xs text-red-500" role="alert">
              {errors.email.message}
            </p>
          )}
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
              <span>Sending…</span>
            </>
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
