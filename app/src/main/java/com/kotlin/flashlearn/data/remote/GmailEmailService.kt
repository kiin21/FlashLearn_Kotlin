package com.kotlin.flashlearn.data.remote

import com.kotlin.flashlearn.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * Service for sending emails via Gmail SMTP.
 * Uses App Password for authentication.
 *
 * Setup:
 * 1. Enable 2FA on your Google Account
 * 2. Generate App Password at https://myaccount.google.com/apppasswords
 * 3. Add GMAIL_EMAIL and GMAIL_APP_PASSWORD to local.properties
 */
@Singleton
class GmailEmailService @Inject constructor() {

    companion object {
        private const val SMTP_HOST = "smtp.gmail.com"
        private const val SMTP_PORT = "587"

        // Firebase Hosting URL - update this after deploying
        private const val HOSTING_BASE_URL = "https://flashlearn-52968.web.app"
    }

    /**
     * Converts deep link to HTTPS link for email clickability.
     * flashlearn://reset-password?token=xxx → https://flashlearn-52968.web.app/reset?token=xxx
     */
    private fun toHttpsLink(deepLink: String): String {
        val token = deepLink.substringAfter("token=", "")
        return "$HOSTING_BASE_URL/reset?token=$token"
    }

    /**
     * Sends a password reset email with a magic link.
     *
     * @param toEmail Recipient email address
     * @param resetLink The deep link for password reset (flashlearn://reset-password?token=xxx)
     * @return Result with success message or error
     */
    suspend fun sendPasswordResetEmail(toEmail: String, resetLink: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val senderEmail = BuildConfig.GMAIL_EMAIL
                val appPassword = BuildConfig.GMAIL_APP_PASSWORD

                if (senderEmail.isBlank() || appPassword.isBlank()) {
                    throw Exception("Gmail credentials not configured. Please set GMAIL_EMAIL and GMAIL_APP_PASSWORD in local.properties")
                }

                val properties = Properties().apply {
                    put("mail.smtp.host", SMTP_HOST)
                    put("mail.smtp.port", SMTP_PORT)
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.ssl.protocols", "TLSv1.2")
                }

                val session = Session.getInstance(properties, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(senderEmail, appPassword)
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(senderEmail, "FlashLearn"))
                    setRecipient(Message.RecipientType.TO, InternetAddress(toEmail))
                    subject = "Reset Your FlashLearn Password"
                    setContent(buildPasswordResetEmailHtml(resetLink), "text/html; charset=utf-8")
                }

                Transport.send(message)
            }
        }

    private fun buildPasswordResetEmailHtml(deepLink: String): String {
        // Convert deep link to HTTPS link for email clickability
        val httpsLink = toHttpsLink(deepLink)

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; 
                         background-color: #f4f4f5; margin: 0; padding: 40px 20px;">
                <div style="max-width: 480px; margin: 0 auto; background: white; border-radius: 12px; 
                            box-shadow: 0 4px 6px rgba(0,0,0,0.1); overflow: hidden;">
                    
                    <!-- Header -->
                    <div style="background: linear-gradient(135deg, #E5434F 0%, #ff6b6b 100%); 
                                padding: 32px; text-align: center;">
                        <h1 style="color: white; margin: 0; font-size: 28px;">🔒 FlashLearn</h1>
                    </div>
                    
                    <!-- Body -->
                    <div style="padding: 32px;">
                        <h2 style="color: #1f2937; margin: 0 0 16px 0; font-size: 20px;">
                            Reset Your Password
                        </h2>
                        <p style="color: #6b7280; line-height: 1.6; margin: 0 0 24px 0;">
                            We received a request to reset your password. Click the button below to create a new password inside the app:
                        </p>
                        
                        <!-- CTA Button -->
                        <div style="text-align: center; margin: 32px 0;">
                            <a href="$httpsLink" 
                               style="display: inline-block; background: #E5434F; color: white; 
                                      padding: 14px 32px; text-decoration: none; border-radius: 8px; 
                                      font-weight: 600; font-size: 16px; box-shadow: 0 4px 6px rgba(229, 67, 79, 0.2);">
                                Reset Password
                            </a>
                        </div>
                        
                        <p style="color: #9ca3af; font-size: 14px; line-height: 1.5; margin: 24px 0 0 0;">
                            ⏰ This link expires in <strong>1 hour</strong>.<br>
                            If you didn't request this, you can safely ignore this email.
                        </p>
                    </div>
                    
                    <!-- Footer -->
                    <div style="background: #f9fafb; padding: 20px; text-align: center; 
                                border-top: 1px solid #e5e7eb;">
                        <p style="color: #9ca3af; font-size: 12px; margin: 0;">
                            © 2024 FlashLearn. Learn smarter, not harder.
                        </p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
