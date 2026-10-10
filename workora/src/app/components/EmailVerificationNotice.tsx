'use client';
import React, { useState, useEffect } from 'react';
import { Mail, ArrowLeft, RefreshCw, CheckCircle, Clock } from 'lucide-react';
import { toast } from 'sonner';
import { resendVerificationEmail, checkVerificationSession } from '@/lib/auth';
import { subscribeToEmailVerificationComplete } from '@/lib/email-verification-channel';
import { AxiosError } from 'axios';

interface Props {
  email: string;
  verificationSessionToken?: string;
  onBack: () => void;
  onVerified?: () => void;
}

function maskEmailAddress(email: string): string {
  const separator = email.lastIndexOf('@');
  if (separator < 1) return email;

  const localPart = email.slice(0, separator);
  const visibleLocalPart =
    localPart.length < 3
      ? `${localPart[0]}***`
      : `${localPart[0]}***${localPart[localPart.length - 1]}`;
  return `${visibleLocalPart}${email.slice(separator)}`;
}

export default function EmailVerificationNotice({
  email,
  verificationSessionToken,
  onBack,
  onVerified,
}: Props) {
  const [resending, setResending] = useState(false);
  const [resent, setResent] = useState(false);
  const [resendCooldownSeconds, setResendCooldownSeconds] = useState(60);
  const [verificationStatus, setVerificationStatus] = useState<
    'PENDING' | 'VERIFIED' | 'EXPIRED' | null
  >(null);

  useEffect(() => {
    if (resendCooldownSeconds === 0) {
      setResent(false);
      return;
    }
    const interval = setInterval(() => {
      setResendCooldownSeconds((remaining) => Math.max(remaining - 1, 0));
    }, 1000);
    return () => clearInterval(interval);
  }, [resendCooldownSeconds]);

  // Poll verification status if we have a session token
  useEffect(() => {
    if (!verificationSessionToken) return;

    let pollInProgress = false;
    let sessionExpired = false;
    let verificationSignalQueued = false;
    const poll = async (verificationSignalReceived = false) => {
      if (pollInProgress) {
        verificationSignalQueued ||= verificationSignalReceived;
        return;
      }
      if (sessionExpired && !verificationSignalReceived) return;
      pollInProgress = true;
      try {
        const result = await checkVerificationSession(verificationSessionToken);
        setVerificationStatus(result.status);
        sessionExpired = result.status === 'EXPIRED';
        if (result.status === 'VERIFIED') {
          toast.success('Email verified! You can now sign in.');
          onVerified?.();
        }
      } catch {
        // Silently ignore polling errors
      } finally {
        pollInProgress = false;
        if (verificationSignalQueued) {
          verificationSignalQueued = false;
          void poll(true);
        }
      }
    };

    poll();
    const interval = setInterval(() => poll(), 5000);
    const unsubscribe = subscribeToEmailVerificationComplete(() => poll(true));
    return () => {
      clearInterval(interval);
      unsubscribe();
    };
  }, [verificationSessionToken, onVerified]);

  const handleResend = async () => {
    setResending(true);
    try {
      await resendVerificationEmail(email);
      setResent(true);
      setResendCooldownSeconds(60);
      toast.success('Verification email resent successfully');
    } catch (err: unknown) {
      let message = 'Failed to resend verification email. Please try again.';
      if (err instanceof AxiosError) {
        const apiMsg = err.response?.data?.message;
        if (apiMsg) message = apiMsg;
        else if (!err.response)
          message = 'Cannot reach the server. Make sure the backend is running on localhost:8080.';
      }
      toast.error(message);
    } finally {
      setResending(false);
    }
  };

  return (
    <div className="fade-scale-in text-center">
      <div className="w-16 h-16 bg-blue-50 rounded-2xl flex items-center justify-center mx-auto mb-6">
        <Mail size={28} className="text-primary" />
      </div>

      <h2 className="text-2xl font-bold text-foreground mb-2">Check Your Email</h2>
      <p className="text-muted-foreground text-sm leading-relaxed mb-2">
        We&apos;ve sent a verification link to
      </p>
      <p
        className="font-semibold text-foreground text-sm mb-6 bg-muted px-3 py-2 rounded-lg inline-block"
        aria-label="Verification email address"
      >
        {maskEmailAddress(email)}
      </p>

      {/* Verification status indicator */}
      {verificationSessionToken && (
        <div
          role="status"
          aria-live="polite"
          className={`mb-4 px-4 py-2.5 rounded-lg border text-sm flex items-center justify-center gap-2 ${
            verificationStatus === 'VERIFIED'
              ? 'bg-green-50 border-green-200 text-green-700'
              : verificationStatus === 'EXPIRED'
                ? 'bg-red-50 border-red-200 text-red-700'
                : 'bg-amber-50 border-amber-200 text-amber-700'
          }`}
        >
          {verificationStatus === 'VERIFIED' ? (
            <>
              <CheckCircle size={14} />
              <span>Email verified!</span>
            </>
          ) : verificationStatus === 'EXPIRED' ? (
            <>
              <Clock size={14} />
              <span>Session expired — please resend</span>
            </>
          ) : (
            <>
              <RefreshCw size={14} className="animate-spin" />
              <span>Waiting for verification…</span>
            </>
          )}
        </div>
      )}

      <div className="bg-blue-50 border border-blue-100 rounded-xl p-4 mb-6 text-left">
        <div className="flex gap-3">
          <CheckCircle size={16} className="text-primary flex-shrink-0 mt-0.5" />
          <div>
            <p className="text-sm font-medium text-foreground mb-1">What to do next</p>
            <ol className="text-xs text-muted-foreground space-y-1 list-decimal list-inside">
              <li>
                Open the email from Workora. Check your spam or junk folder if it is not in your
                inbox.
              </li>
              <li>Click the &quot;Verify Email&quot; button in the email</li>
              <li>Continue back to Workora and sign in after the confirmation appears</li>
            </ol>
          </div>
        </div>
      </div>

      <button
        onClick={handleResend}
        disabled={resending || resent || resendCooldownSeconds > 0}
        className="btn-secondary w-full py-2.5 rounded-lg font-medium text-sm flex items-center justify-center gap-2 mb-4 disabled:opacity-60 disabled:cursor-not-allowed"
      >
        {resending ? (
          <>
            <RefreshCw size={14} className="animate-spin" />
            <span>Resending…</span>
          </>
        ) : resent ? (
          <>
            <CheckCircle size={14} className="text-green-500" />
            <span>Email resent</span>
          </>
        ) : resendCooldownSeconds > 0 ? (
          <>
            <Clock size={14} />
            <span>Resend available in {resendCooldownSeconds}s</span>
          </>
        ) : (
          <>
            <RefreshCw size={14} />
            <span>Resend verification email</span>
          </>
        )}
      </button>

      <button
        onClick={onBack}
        className="flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors mx-auto"
      >
        <ArrowLeft size={14} />
        Back to sign in
      </button>
    </div>
  );
}
