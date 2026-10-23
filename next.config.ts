import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  cacheComponents: true,
  partialPrefetching: true,
  experimental: {
    // Incident photos are uploaded through Server Actions; match the backend's 200 MB request limit.
    serverActions: { bodySizeLimit: "201mb" },
  },
};

export default nextConfig;
