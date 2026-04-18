# AGENTS

<skills_system priority="1">

## Available Skills

<!-- SKILLS_TABLE_START -->
<usage>
When users ask you to perform tasks, check if any of the available skills below can help complete the task more effectively. Skills provide specialized capabilities and domain knowledge.

How to use skills:
- Invoke: `npx openskills read <skill-name>` (run in your shell)
  - For multiple: `npx openskills read skill-one,skill-two`
- The skill content will load with detailed instructions on how to complete the task
- Base directory provided in output for resolving bundled resources (references/, scripts/, assets/)

Usage notes:
- Only use skills listed in <available_skills> below
- Do not invoke a skill that is already loaded in your context
- Each skill invocation is stateless
</usage>

<available_skills>

<skill>
<name>agent-device</name>
<description>Automates interactions for Apple-platform apps (iOS, tvOS, macOS) and Android devices. Use when navigating apps, taking snapshots/screenshots, tapping, typing, scrolling, or extracting UI info across mobile, TV, and desktop targets.</description>
<location>global</location>
</skill>

<skill>
<name>api-routes</name>
<description>Guidelines for creating API routes in Expo Router with EAS Hosting</description>
<location>global</location>
</skill>

<skill>
<name>banner-design</name>
<description>"Design banners for social media, ads, website heroes, creative assets, and print. Multiple art direction options with AI-generated visuals. Actions: design, create, generate banner. Platforms: Facebook, Twitter/X, LinkedIn, YouTube, Instagram, Google Display, website hero, print. Styles: minimalist, gradient, bold typography, photo-based, illustrated, geometric, retro, glassmorphism, 3D, neon, duotone, editorial, collage. Uses ui-ux-pro-max, frontend-design, ai-artist, ai-multimodal skills."</description>
<location>global</location>
</skill>

<skill>
<name>brand</name>
<description>Brand voice, visual identity, messaging frameworks, asset management, brand consistency. Activate for branded content, tone of voice, marketing assets, brand compliance, style guides.</description>
<location>global</location>
</skill>

<skill>
<name>building-native-ui</name>
<description>Complete guide for building beautiful apps with Expo Router. Covers fundamentals, styling, components, navigation, animations, patterns, and native tabs.</description>
<location>global</location>
</skill>

<skill>
<name>building-ui</name>
<description>Complete guide for building beautiful apps with Expo Router. Covers fundamentals, styling, components, navigation, animations, patterns, and native tabs.</description>
<location>global</location>
</skill>

<skill>
<name>claude-android-ninja</name>
<description>Create production-quality Android applications following Google's official Android architecture guidance with Kotlin, Jetpack Compose, MVVM architecture, Hilt dependency injection, Room 3 local persistence (KSP, SQLiteDriver, Flow/suspend DAOs), and multi-module architecture. Triggers on requests to create Android projects, modules, screens, ViewModels, repositories, or when asked about Android architecture patterns and best practices.</description>
<location>global</location>
</skill>

<skill>
<name>composition-patterns</name>
<description>React composition patterns that scale. Use when refactoring components with</description>
<location>global</location>
</skill>

<skill>
<name>data-fetching</name>
<description>Use when implementing or debugging ANY network request, API call, or data fetching. Covers fetch API, axios, React Query, SWR, error handling, caching strategies, offline support.</description>
<location>global</location>
</skill>

<skill>
<name>design</name>
<description>"Comprehensive design skill: brand identity, design tokens, UI styling, logo generation (55 styles, Gemini AI), corporate identity program (50 deliverables, CIP mockups), HTML presentations (Chart.js), banner design (22 styles, social/ads/web/print), icon design (15 styles, SVG, Gemini 3.1 Pro), social photos (HTML→screenshot, multi-platform). Actions: design logo, create CIP, generate mockups, build slides, design banner, generate icon, create social photos, social media images, brand identity, design system. Platforms: Facebook, Twitter, LinkedIn, YouTube, Instagram, Pinterest, TikTok, Threads, Google Ads."</description>
<location>global</location>
</skill>

<skill>
<name>design-system</name>
<description>Token architecture, component specifications, and slide generation. Three-layer tokens (primitive→semantic→component), CSS variables, spacing/typography scales, component specs, strategic slide creation. Use for design tokens, systematic design, brand-compliant presentations.</description>
<location>global</location>
</skill>

<skill>
<name>dev-client</name>
<description>Build and distribute Expo development clients locally or via TestFlight</description>
<location>global</location>
</skill>

<skill>
<name>dogfood</name>
<description>'Systematically explore and test a mobile app on iOS/Android with agent-device to find bugs, UX issues, and other problems. Use when asked to "dogfood", "QA", "exploratory test", "find issues", "bug hunt", or "test this app" on mobile. Produces a structured report with reproducible evidence: screenshots, optional repro videos, and detailed steps for every issue.'</description>
<location>global</location>
</skill>

<skill>
<name>expo-api-routes</name>
<description>Guidelines for creating API routes in Expo Router with EAS Hosting</description>
<location>global</location>
</skill>

<skill>
<name>expo-cicd-workflows</name>
<description>Helps understand and write EAS workflow YAML files for Expo projects. Use this skill when the user asks about CI/CD or workflows in an Expo or EAS context, mentions .eas/workflows/, or wants help with EAS build pipelines or deployment automation.</description>
<location>global</location>
</skill>

<skill>
<name>expo-deployment</name>
<description>Deploying Expo apps to iOS App Store, Android Play Store, web hosting, and API routes</description>
<location>global</location>
</skill>

<skill>
<name>expo-dev-client</name>
<description>Build and distribute Expo development clients locally or via TestFlight</description>
<location>global</location>
</skill>

<skill>
<name>expo-tailwind-setup</name>
<description>Set up Tailwind CSS v4 in Expo with react-native-css and NativeWind v5 for universal styling</description>
<location>global</location>
</skill>

<skill>
<name>expo-ui-jetpack-compose</name>
<description>`@expo/ui/jetpack-compose` package lets you use Jetpack Compose Views and modifiers in your app.</description>
<location>global</location>
</skill>

<skill>
<name>expo-ui-swift-ui</name>
<description>`@expo/ui/swift-ui` package lets you use SwiftUI Views and modifiers in your app.</description>
<location>global</location>
</skill>

<skill>
<name>github</name>
<description>GitHub patterns using gh CLI for pull requests, stacked PRs, code review, branching strategies, and repository automation. Use when working with GitHub PRs, merging strategies, or repository management tasks.</description>
<location>global</location>
</skill>

<skill>
<name>github-actions</name>
<description>GitHub Actions workflow patterns for React Native iOS simulator and Android emulator cloud builds with downloadable artifacts. Use when setting up CI build pipelines or downloading GitHub Actions artifacts via gh CLI and GitHub API.</description>
<location>global</location>
</skill>

<skill>
<name>native-data-fetching</name>
<description>Use when implementing or debugging ANY network request, API call, or data fetching. Covers fetch API, React Query, SWR, error handling, caching, offline support, and Expo Router data loaders (`useLoaderData`).</description>
<location>global</location>
</skill>

<skill>
<name>react-best-practices</name>
<description>React and Next.js performance optimization guidelines from Vercel Engineering. This skill should be used when writing, reviewing, or refactoring React/Next.js code to ensure optimal performance patterns. Triggers on tasks involving React components, Next.js pages, data fetching, bundle optimization, or performance improvements.</description>
<location>global</location>
</skill>

<skill>
<name>react-native</name>
<description>Complete React Native and Expo optimization guide combining Callstack profiling with Vercel code patterns. Covers FPS, TTI, bundle size, memory, lists, animations, state, UI, and React Compiler. Use for any React Native performance, debugging, or best practices task.</description>
<location>global</location>
</skill>

<skill>
<name>react-native-best-practices</name>
<description>Provides React Native performance optimization guidelines for FPS, TTI, bundle size, memory leaks, re-renders, and animations. Applies to tasks involving Hermes optimization, JS thread blocking, bridge overhead, FlashList, native modules, or debugging jank and frame drops.</description>
<location>global</location>
</skill>

<skill>
<name>react-native-brownfield-migration</name>
<description>Provides an incremental adoption strategy to migrate native iOS or Android apps to React Native or Expo using @callstack/react-native-brownfield for initial setup. Use when planning migration steps, packaging XCFramework/AAR artifacts, and integrating them into host apps.</description>
<location>global</location>
</skill>

<skill>
<name>react-native-testing</name>
<description>></description>
<location>global</location>
</skill>

<skill>
<name>slides</name>
<description>Create strategic HTML presentations with Chart.js, design tokens, responsive layouts, copywriting formulas, and contextual slide strategies.</description>
<location>global</location>
</skill>

<skill>
<name>tailwind-setup</name>
<description>Set up Tailwind CSS v4 in Expo with react-native-css and NativeWind v5 for universal styling</description>
<location>global</location>
</skill>

<skill>
<name>ui-styling</name>
<description>Create beautiful, accessible user interfaces with shadcn/ui components (built on Radix UI + Tailwind), Tailwind CSS utility-first styling, and canvas-based visual designs. Use when building user interfaces, implementing design systems, creating responsive layouts, adding accessible components (dialogs, dropdowns, forms, tables), customizing themes and colors, implementing dark mode, generating visual designs and posters, or establishing consistent styling patterns across applications.</description>
<location>global</location>
</skill>

<skill>
<name>ui-ux-pro-max</name>
<description>"UI/UX design intelligence for web and mobile. Includes 50+ styles, 161 color palettes, 57 font pairings, 161 product types, 99 UX guidelines, and 25 chart types across 10 stacks (React, Next.js, Vue, Svelte, SwiftUI, React Native, Flutter, Tailwind, shadcn/ui, and HTML/CSS). Actions: plan, build, create, design, implement, review, fix, improve, optimize, enhance, refactor, and check UI/UX code. Projects: website, landing page, dashboard, admin panel, e-commerce, SaaS, portfolio, blog, and mobile app. Elements: button, modal, navbar, sidebar, card, table, form, and chart. Styles: glassmorphism, claymorphism, minimalism, brutalism, neumorphism, bento grid, dark mode, responsive, skeuomorphism, and flat design. Topics: color systems, accessibility, animation, layout, typography, font pairing, spacing, interaction states, shadow, and gradient. Integrations: shadcn/ui MCP for component search and examples."</description>
<location>global</location>
</skill>

<skill>
<name>upgrading-expo</name>
<description>Guidelines for upgrading Expo SDK versions and fixing dependency issues</description>
<location>global</location>
</skill>

<skill>
<name>upgrading-react-native</name>
<description>Upgrades React Native apps to newer versions by applying rn-diff-purge template diffs, updating package.json dependencies, migrating native iOS and Android configuration, resolving CocoaPods and Gradle changes, and handling breaking API updates. Use when upgrading React Native, bumping RN version, updating from RN 0.x to 0.y, or migrating Expo SDK alongside a React Native upgrade.</description>
<location>global</location>
</skill>

<skill>
<name>use-dom</name>
<description>Use Expo DOM components to run web code in a webview on native and as-is on web. Migrate web code to native incrementally.</description>
<location>global</location>
</skill>

<skill>
<name>validate-skills</name>
<description>Validates skills in this repo against agentskills.io spec and Claude Code best practices. Use via /validate-skills command.</description>
<location>global</location>
</skill>

<skill>
<name>vercel-react-native-skills</name>
<description>React Native and Expo best practices for building performant mobile apps. Use</description>
<location>global</location>
</skill>

</available_skills>
<!-- SKILLS_TABLE_END -->

</skills_system>
