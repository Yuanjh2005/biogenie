"use client";

import { type ReactNode, useState } from "react";
import { useRouter } from "next/navigation";
import { AlertCircle, Eye, EyeOff } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { authApi } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/errors";
import { LOGIN_COPY } from "@/lib/i18n/login";
import { useAuthStore } from "@/stores/auth";
import { useLocaleStore } from "@/stores/locale";

const USERNAME_RE = /^[A-Za-z0-9_.-]{4,32}$/;
const STRONG_PASSWORD_RE = /^(?=.*[A-Za-z])(?=.*\d).{8,72}$/;

function ValidationHint({ children }: { children: ReactNode }) {
  return (
    <li className="flex items-center gap-1.5 text-destructive">
      <AlertCircle className="size-3.5" />
      <span>{children}</span>
    </li>
  );
}

export function PasswordLoginForm({ afterSubmit }: { afterSubmit?: ReactNode }) {
  const router = useRouter();
  const setSession = useAuthStore((s) => s.setSession);
  const copy = LOGIN_COPY[useLocaleStore((s) => s.locale)];
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [registering, setRegistering] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [registrationAttempted, setRegistrationAttempted] = useState(false);
  const usernameValid = USERNAME_RE.test(username);
  const passwordLengthValid = password.length >= 8 && password.length <= 72;
  const passwordCompositionValid = /[A-Za-z]/.test(password) && /\d/.test(password);
  const passwordsMatch = password === confirmPassword;
  const valid = usernameValid && (registering
    ? STRONG_PASSWORD_RE.test(password) && passwordsMatch
    : password.length >= 8 && password.length <= 72);

  const submit = async () => {
    if (submitting) return;
    if (!valid) {
      if (registering) setRegistrationAttempted(true);
      return;
    }
    setSubmitting(true);
    try {
      const result = registering
        ? await authApi.registerByPassword(username.toLowerCase(), password)
        : await authApi.loginByPassword(username.toLowerCase(), password);
      setSession(result);
      toast.success(registering ? copy.registerSuccess : copy.loginSuccess);
      router.replace("/home");
    } catch (error) {
      toast.error(error instanceof ApiError ? error.message : copy.loginFailed);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-5">
      <Input
        aria-label={copy.username}
        placeholder={copy.usernamePlaceholder}
        value={username}
        maxLength={32}
        autoComplete="username"
        onChange={(event) => setUsername(event.target.value.replace(/[^A-Za-z0-9_.-]/g, ""))}
        className="h-12 rounded-xl bg-muted/40 px-4"
      />
      {registering && registrationAttempted && !usernameValid && (
        <ul className="-mt-3 px-1 text-xs">
          <ValidationHint>{copy.usernameRequirement}</ValidationHint>
        </ul>
      )}
      <div className="relative">
        <Input
          aria-label={copy.password}
          type={showPassword ? "text" : "password"}
          placeholder={copy.passwordPlaceholder}
          value={password}
          maxLength={72}
          autoComplete={registering ? "new-password" : "current-password"}
          onChange={(event) => setPassword(event.target.value)}
          onKeyDown={(event) => event.key === "Enter" && submit()}
          className="h-12 rounded-xl bg-muted/40 px-4 pr-12"
        />
        <button
          type="button"
          aria-label={showPassword ? copy.hidePassword : copy.showPassword}
          onClick={() => setShowPassword((value) => !value)}
          className="absolute right-4 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
        >
          {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
        </button>
      </div>
      {registering && registrationAttempted && (!passwordLengthValid || !passwordCompositionValid) && (
        <ul className="-mt-3 space-y-1 px-1 text-xs">
          {!passwordLengthValid && <ValidationHint>{copy.passwordLengthRequirement}</ValidationHint>}
          {!passwordCompositionValid && <ValidationHint>{copy.passwordCompositionRequirement}</ValidationHint>}
        </ul>
      )}
      {registering && (
        <div className="space-y-1.5">
          <div className="relative">
            <Input
              aria-label={copy.confirmPassword}
              aria-invalid={registrationAttempted && !passwordsMatch}
              type={showPassword ? "text" : "password"}
              placeholder={copy.confirmPasswordPlaceholder}
              value={confirmPassword}
              maxLength={72}
              autoComplete="new-password"
              onChange={(event) => setConfirmPassword(event.target.value)}
              onKeyDown={(event) => event.key === "Enter" && submit()}
              className="h-12 rounded-xl bg-muted/40 px-4 pr-12"
            />
            <button
              type="button"
              aria-label={showPassword ? copy.hidePassword : copy.showPassword}
              onClick={() => setShowPassword((value) => !value)}
              className="absolute right-4 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
            >
              {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
            </button>
          </div>
          {registrationAttempted && !passwordsMatch && (
            <p className="px-1 text-xs text-destructive">{copy.passwordMismatch}</p>
          )}
        </div>
      )}
      <Button
        type="button"
        className="h-11 w-full rounded-xl bg-brand text-brand-foreground hover:bg-brand/90"
        disabled={submitting || (!registering && !valid)}
        onClick={submit}
      >
        {submitting ? (registering ? copy.registering : copy.loggingIn) : (registering ? copy.register : copy.login)}
      </Button>
      {afterSubmit}
      <p className="text-center text-sm text-muted-foreground">
        {registering ? copy.hasAccount : copy.noAccount}
        <button
          type="button"
          className="ml-1 font-medium text-brand hover:underline"
          onClick={() => {
            setRegistering((value) => !value);
            setConfirmPassword("");
            setRegistrationAttempted(false);
          }}
        >
          {registering ? copy.passwordLogin : copy.register}
        </button>
      </p>
    </div>
  );
}
