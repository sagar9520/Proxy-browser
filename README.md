# Proxy-browser
New private proxy browser

# ProxyGo Browser (com.myfuturecode.proxygo)

Proxy browser: Splash → Home (quick sites) → Proxy list → Connected popup → Menu → Settings.
Sirf is browser ka traffic proxy se jaata hai (WebView ProxyController). VPN permission nahi chahiye.

## Remote config
App `CONFIG_URL?file=com.myfuturecode.proxygo` (header `X-App-Key`) se config leta hai.
Server pe `sagar_apps/com.myfuturecode.proxygo/data.json` banao, format = `app/src/main/assets/default_config.json`.

- `proxies`: `name, country (US/DE/..), type (http/https/socks5), host, port, user, pass, quality (high/medium/low), premium`
  - `host` khali ho to server list me nahi dikhta. **Apne proxy servers ka IP yahan daalna zaroori hai.**
  - `premium: true` = rewarded ad dekh ke unlock.
- `quick_sites`: home ke icons (`name, url, letter, color, icon` url optional)
- `adblock_hosts`: websites ke andar ke ads block
- `ads`: `enabled`, `network` (`admob` / `max`), `interstitial_every`, `app_open`, aur unit ids
- `privacy_url`, `cross_promo`

## Build secrets (GitHub → Settings → Secrets → Actions)
`CONFIG_APP_KEY` (get_json.php ki key), `ADMOB_APP_ID`, `APPLOVIN_SDK_KEY`. Na do to test ads chalenge, config asset wala.

## Proxy server kaise banaye (sasta)
Koi bhi VPS (₹300-400/maah) pe Squid: `apt install squid`, `/etc/squid/squid.conf` me `http_port 3128`
aur basic auth (htpasswd) laga do, phir IP/port/user/pass `data.json` ke `proxies` me daal do.
Ya Webshare/Proxy-Cheap jaise paid proxy provider ke HTTP proxies bhi chalenge.
