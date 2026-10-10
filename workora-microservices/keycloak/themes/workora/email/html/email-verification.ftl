<#import "template.ftl" as layout>
<@layout.emailLayout>
  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="max-width:600px;margin:0 auto;font-family:'Segoe UI',Arial,sans-serif;color:#0f172a;">
    <tr>
      <td style="padding:24px 24px 8px;text-align:center;">
        <img src="${url.resourcesUrl}/img/workora-mark.png" width="52" height="52" alt="Workora" style="display:block;width:52px;height:52px;margin:0 auto 12px;border:0;">
        <div style="font-size:22px;font-weight:700;letter-spacing:-.4px;">Workora</div>
      </td>
    </tr>
    <tr>
      <td style="padding:24px;background:#ffffff;border:1px solid #e2e8f0;border-radius:14px;">
        <h1 style="margin:0 0 18px;font-size:24px;line-height:1.3;">Verify your email address</h1>
        <#if user.firstName?? && user.firstName?has_content>
          <p style="margin:0 0 14px;font-size:15px;line-height:1.6;">Hello ${user.firstName},</p>
        <#else>
          <p style="margin:0 0 14px;font-size:15px;line-height:1.6;">Hello,</p>
        </#if>
        <p style="margin:0 0 22px;color:#475569;font-size:15px;line-height:1.6;">
          Welcome to Workora. Verify this email address to complete your registration and continue to your account.
        </p>
        <table role="presentation" cellspacing="0" cellpadding="0" border="0" style="margin:0 auto 22px;">
          <tr>
            <td bgcolor="#2563eb" style="border-radius:8px;text-align:center;">
              <a href="${link}" style="display:inline-block;padding:13px 22px;border:1px solid #2563eb;border-radius:8px;color:#ffffff;font-size:15px;font-weight:600;text-decoration:none;">Verify Email</a>
            </td>
          </tr>
        </table>
        <p style="margin:0 0 8px;color:#475569;font-size:13px;line-height:1.6;">
          If the button does not work, open this verification link:
        </p>
        <p style="margin:0 0 22px;overflow-wrap:anywhere;font-size:13px;line-height:1.6;">
          <a href="${link}" style="color:#2563eb;">${link}</a>
        </p>
        <p style="margin:0 0 14px;color:#475569;font-size:13px;line-height:1.6;">
          This link expires in ${linkExpirationFormatter(linkExpiration)}.
        </p>
        <p style="margin:0;color:#64748b;font-size:13px;line-height:1.6;">
          If you did not create a Workora account, you can safely ignore this email.
        </p>
      </td>
    </tr>
    <tr>
      <td style="padding:18px 12px;text-align:center;color:#64748b;font-size:12px;line-height:1.5;">
        The Workora Team
      </td>
    </tr>
  </table>
</@layout.emailLayout>
