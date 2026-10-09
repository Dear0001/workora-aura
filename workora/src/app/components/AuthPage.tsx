'use client';
import React, { useState } from 'react';
import LoginForm from './LoginForm';
import RegisterForm from './RegisterForm';
import EmailVerificationNotice from './EmailVerificationNotice';
import ForgotPasswordForm from './ForgotPasswordForm';

import AppLogo from '@/components/ui/AppLogo';
import { CheckCircle, Users, Zap, Shield } from 'lucide-react';

type AuthView = 'login' | 'register' | 'verify-email' | 'forgot-password';

const FEATURES = [
  { icon: Users, text: 'Unified team & company management' },
  { icon: Zap, text: 'Real-time project & task tracking' },
  { icon: Shield, text: 'Enterprise-grade security via Keycloak JWT' },
  { icon: CheckCircle, text: 'Integrated bug triage & recruitment flows' },
];

export default function AuthPage() {
  const [view, setView] = useState<AuthView>('login');
  const [registeredEmail, setRegisteredEmail] = useState('');
  const [verificationSessionToken, setVerificationSessionToken] = useState<string | undefined>();

  const handleRegistrationSuccess = (email: string, sessionToken?: string) => {
    setRegisteredEmail(email);
    setVerificationSessionToken(sessionToken);
    setView('verify-email');
  };

  const handleBackToLogin = () => {
    setView('login');
  };

  const handleVerified = () => {
    setView('login');
  };

  return (
    <div className="min-h-screen flex">
      {/* Left brand panel */}
      <div
        className="hidden lg:flex lg:w-[480px] xl:w-[560px] flex-col justify-between p-10 xl:p-14 relative overflow-hidden flex-shrink-0"
        style={{ background: 'linear-gradient(145deg, #1E3A5F 0%, #1e40af 45%, #4c1d95 100%)' }}
      >
        {/* Background decoration */}
        <div className="absolute inset-0 opacity-10">
          <div className="absolute top-20 -left-10 w-64 h-64 rounded-full border border-white/30" />
          <div className="absolute top-40 -left-5 w-40 h-40 rounded-full border border-white/20" />
          <div className="absolute bottom-32 -right-16 w-80 h-80 rounded-full border border-white/20" />
          <div className="absolute bottom-10 right-10 w-48 h-48 rounded-full border border-white/15" />
        </div>

        {/* Logo */}
        <div className="relative z-10 flex items-center gap-3">
          <AppLogo size={40} />
          <span className="text-white font-bold text-xl tracking-tight">Workora</span>
        </div>

        {/* Hero content */}
        <div className="relative z-10">
          <h1 className="text-3xl xl:text-4xl font-bold text-white leading-tight mb-4">
            The unified platform for engineering teams
          </h1>
          <p className="text-blue-200 text-base leading-relaxed mb-10">
            Manage projects, track tasks, triage bugs, hire talent, and process payments — all connected through one gateway.
          </p>

          <div className="space-y-4">
            {FEATURES.map((f, i) => (
              <div key={`feature-${i}`} className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-lg bg-white/10 flex items-center justify-center flex-shrink-0">
                  <f.icon size={16} className="text-blue-200" />
                </div>
                <span className="text-blue-100 text-sm">{f.text}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Bottom stats */}
        <div className="relative z-10 grid grid-cols-3 gap-4">
          {[
            { value: '2,400+', label: 'Active Teams' },
            { value: '48K+', label: 'Tasks Tracked' },
            { value: '99.9%', label: 'Uptime SLA' },
          ].map((s) => (
            <div key={`stat-${s.label}`} className="text-center">
              <div className="text-white font-bold text-xl font-tabular">{s.value}</div>
              <div className="text-blue-300 text-xs mt-0.5">{s.label}</div>
            </div>
          ))}
        </div>
      </div>

      {/* Right form panel */}
      <div className="flex-1 flex items-center justify-center p-6 sm:p-10 bg-background">
        <div className="w-full max-w-[440px]">
          {/* Mobile logo */}
          <div className="flex items-center gap-2 mb-8 lg:hidden">
            <AppLogo size={32} />
            <span className="font-bold text-lg text-foreground">Workora</span>
          </div>

          {view === 'verify-email' ? (
            <EmailVerificationNotice
              email={registeredEmail}
              verificationSessionToken={verificationSessionToken}
              onBack={handleBackToLogin}
              onVerified={handleVerified}
            />
          ) : view === 'forgot-password' ? (
            <ForgotPasswordForm onBack={handleBackToLogin} />
          ) : (
            <>
              {/* Tabs */}
              <div className="flex gap-1 p-1 bg-muted rounded-lg mb-8">
                {(['login', 'register'] as const).map((tab) => (
                  <button
                    key={`tab-${tab}`}
                    onClick={() => setView(tab)}
                    className={`flex-1 py-2 px-4 rounded-md text-sm font-semibold transition-all duration-150 ${
                      view === tab
                        ? 'bg-card text-foreground shadow-card'
                        : 'text-muted-foreground hover:text-foreground'
                    }`}
                  >
                    {tab === 'login' ? 'Sign In' : 'Create Account'}
                  </button>
                ))}
              </div>

              {view === 'login' ? (
                <LoginForm
                  onSwitchToRegister={() => setView('register')}
                  onForgotPassword={() => setView('forgot-password')}
                />
              ) : (
                <RegisterForm
                  onSuccess={handleRegistrationSuccess}
                  onSwitchToLogin={() => setView('login')}
                />
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}