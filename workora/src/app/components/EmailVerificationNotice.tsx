'use client';
import React, { useState, useEffect } from 'react';
import { Mail, ArrowLeft, RefreshCw, CheckCircle, Clock } from 'lucide-react';
import { toast } from 'sonner';
import { resendVerificationEmail, checkVerificationSession } from '@/lib/auth';
import { AxiosError } from 'axios';

interface Props {
  email: string;
  verificationSessionToken?: string;
  onBack: () => void;
  onVerified?: () => void;
}

export default function EmailVerificationNotice({ email, verificationSessionToken, onBack, onVerified }: Props) {
  const [resending, setResending] = useState(false);
  const [resent, setResent] = useState(false);
  const [verificationStatus, setVerificationStatus] = useState<'PENDING' | 'VERIFIED' | 'EXPIRED' | null>(null);

  // Poll verification status if we have a session token
  useEffect(() => {
    if (!verificationSessionToken) return;

    const poll = async () => {
      try {
        const result = await checkVerificationSession(verificationSessionToken);
        setVerificationStatus(result.status);
        if (result.status === 'VERIFIED') {
          toast.success('Email verified! You can now sign in.');
          onVerified?.();
        }
      } catch {
        // Silently ignore polling errors
      }
    };

    poll();
    const interval = setInterval(poll, 5000);
    return () => clearInterval(interval);
  }, [verificationSessionToken, onVerified]);

  const handleResend = async () => {
    setResending(true);
    try {
      await resendVerificationEmail(email);
      setResent(true);
      toast.success('Verification email resent successfully');
      setTimeout(() => setResent(false), 5000);
    } catch (err: unknown) {
      let message = 'Failed to resend verification email. Please try again.';
      if (err instanceof AxiosError) {
        const apiMsg = err.response?.data?.message;
        if (apiMsg) message = apiMsg;
        else if (!err.response) message = 'Cannot reach the server. Make sure the backend is running on localhost:8080.';
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

      <h2 className="text-2xl font-bold text-foreground mb-2">Check your inbox</h2>
      <p className="text-muted-foreground text-sm leading-relaxed mb-2">
        We&apos;ve sent a verification link to
      </p>
      <p className="font-semibold text-foreground text-sm mb-6 bg-muted px-3 py-2 rounded-lg inline-block">
        {email}
      </p>

      {/* Verification status indicator */}
      {verificationSessionToken && (
        <div className={`mb-4 px-4 py-2.5 rounded-lg border text-sm flex items-center justify-center gap-2 ${
          verificationStatus === 'VERIFIED' ?'bg-green-50 border-green-200 text-green-700'
            : verificationStatus === 'EXPIRED' ?'bg-red-50 border-red-200 text-red-700' :'bg-amber-50 border-amber-200 text-amber-700'
        }`}>
          {verificationStatus === 'VERIFIED' ? (
            <><CheckCircle size={14} /><span>Email verified!</span></>
          ) : verificationStatus === 'EXPIRED' ? (
            <><Clock size={14} /><span>Session expired — please resend</span></>
          ) : (
            <><RefreshCw size={14} className="animate-spin" /><span>Waiting for verification…</span></>
          )}
        </div>
      )}

      <div className="bg-blue-50 border border-blue-100 rounded-xl p-4 mb-6 text-left">
        <div className="flex gap-3">
          <CheckCircle size={16} className="text-primary flex-shrink-0 mt-0.5" />
          <div>
            <p className="text-sm font-medium text-foreground mb-1">What to do next</p>
            <ol className="text-xs text-muted-foreground space-y-1 list-decimal list-inside">
              <li>Open the email from Workora (check spam if needed)</li>
              <li>Click the &quot;Verify Email&quot; button in the email</li>
              <li>Return here and sign in to your new account</li>
            </ol>
          </div>
        </div>
      </div>

      <button
        onClick={handleResend}
        disabled={resending || resent}
        className="btn-secondary w-full py-2.5 rounded-lg font-medium text-sm flex items-center justify-center gap-2 mb-4 disabled:opacity-60 disabled:cursor-not-allowed"
      >
        {resending ? (
          <><RefreshCw size={14} className="animate-spin" /><span>Resending…</span></>
        ) : resent ? (
          <><CheckCircle size={14} className="text-green-500" /><span>Email resent</span></>
        ) : (
          <><RefreshCw size={14} /><span>Resend verification email</span></>
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