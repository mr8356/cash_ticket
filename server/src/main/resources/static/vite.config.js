import { defineConfig } from 'vite';

export default defineConfig({
    build: {
        outDir: 'dist',          // 최종 번들 폴더
        emptyOutDir: true,       // 빌드 전 dist 비우기
        rollupOptions: {
            input: {
                bid_payment: 'src/bid_payment.js'   // ★ 엔트리 JS 한 개
            },
            output: {
                entryFileNames: 'assets/[name].js'  // dist/assets/bid_payment.js
            }
        }
    }
});