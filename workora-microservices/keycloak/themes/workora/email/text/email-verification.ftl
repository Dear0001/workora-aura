<#ftl output_format="plainText">
<#if user.firstName?? && user.firstName?has_content>
Hello ${user.firstName},
<#else>
Hello,
</#if>

Welcome to Workora. Verify this email address to complete your registration and continue to your account.

Verify your email:
${link}

This link expires in ${linkExpirationFormatter(linkExpiration)}.

If you did not create a Workora account, you can safely ignore this email.

Thank you,
The Workora Team
