export async function getGoogleAccessToken(serviceAccountStr: string): Promise<string | null> {
    try {
        const serviceAccount = JSON.parse(serviceAccountStr);
        
        const header = {
            alg: "RS256",
            typ: "JWT"
        };
    
        const now = Math.floor(Date.now() / 1000);
        const payload = {
            iss: serviceAccount.client_email,
            scope: "https://www.googleapis.com/auth/androidpublisher",
            aud: "https://oauth2.googleapis.com/token",
            exp: now + 3600,
            iat: now
        };
    
        const encoder = new TextEncoder();
        const encodedHeader = btoa(JSON.stringify(header)).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
        const encodedPayload = btoa(JSON.stringify(payload)).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
        const signatureInput = `${encodedHeader}.${encodedPayload}`;
    
        const pem = serviceAccount.private_key.replace(/-----BEGIN PRIVATE KEY-----/, '').replace(/-----END PRIVATE KEY-----/, '').replace(/\s+/g, '');
        const binaryDer = Uint8Array.from(atob(pem), c => c.charCodeAt(0));
    
        const key = await crypto.subtle.importKey(
            "pkcs8",
            binaryDer,
            { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
            false,
            ["sign"]
        );
    
        const signatureBytes = await crypto.subtle.sign(
            "RSASSA-PKCS1-v1_5",
            key,
            encoder.encode(signatureInput)
        );
    
        const encodedSignature = btoa(String.fromCharCode(...new Uint8Array(signatureBytes))).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
        const jwt = `${signatureInput}.${encodedSignature}`;
    
        const resp = await fetch("https://oauth2.googleapis.com/token", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`
        });
        
        if (!resp.ok) {
            console.error("Google Auth failed:", await resp.text());
            return null;
        }
        
        const json: any = await resp.json();
        return json.access_token;
    } catch (e) {
        console.error("Failed to parse or sign Google Credentials:", e);
        return null;
    }
}

export async function verifyGooglePlayPurchase(
    packageName: string,
    productId: string,
    purchaseToken: string,
    isSubscription: boolean,
    accessToken: string
): Promise<{ isValid: boolean; expiryTimeMillis?: number; isAutoRenewing?: boolean }> {
    const apiType = isSubscription ? 'subscriptions' : 'products';
    const url = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/purchases/${apiType}/${productId}/tokens/${purchaseToken}`;
    
    const resp = await fetch(url, {
        method: "GET",
        headers: {
            "Authorization": `Bearer ${accessToken}`,
            "Accept": "application/json"
        }
    });

    if (!resp.ok) {
        console.error("Google Play API Error:", await resp.text());
        return { isValid: false };
    }

    const data: any = await resp.json();
    
    if (isSubscription) {
        // paymentState: 0=pending, 1=received, 2=free trial, 3=pending deferred
        const paymentReceived = data.paymentState === 1 || data.paymentState === 2;
        const nowMs = Date.now();
        const expiresMs = parseInt(data.expiryTimeMillis || "0", 10);
        
        if (paymentReceived && expiresMs > nowMs) {
            return {
                isValid: true,
                expiryTimeMillis: expiresMs,
                isAutoRenewing: data.autoRenewing
            };
        }
    } else {
        // One-time products
        if (data.purchaseState === 0) { // 0 = Purchased, 1 = Canceled, 2 = Pending
            return { isValid: true };
        }
    }

    return { isValid: false };
}
