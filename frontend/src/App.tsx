import { lazy, Suspense, type ReactNode } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { useDemoUser } from './app/DemoUserContext'
import { readAuthSession } from './features/public/authSession'
import { AppShell } from './components/AppShell'
import { Button, EmptyState, Skeleton } from './components/ui'
import LandingPage from './features/public/LandingPage'
import { AbsencePage, BillsPage, DashboardPage, MenuPage } from './features/meal/pages/StudentPages'
import { FinalYearCollectionsPage } from './features/meal/pages/BillingManagementPages'
import { StockHandoverPage, MonthlyReportsPage } from './features/meal/pages/Part12Pages'
import { OrderDetailPage, OrderPage, OrdersPage } from './features/meal/pages/OrderPages'

const ManagerDashboardPage = lazy(() => import('./features/meal/pages/ManagerPages').then((module) => ({ default: module.ManagerDashboardPage })))
const FractionSheetPage = lazy(() => import('./features/meal/pages/FractionSheetPage'))
const CookingPlanPage = lazy(() => import('./features/meal/pages/CookingPlanPage'))
const StockPage = lazy(() => import('./features/meal/pages/StockPage'))
const KitchenIssuePage = lazy(() => import('./features/meal/pages/KitchenIssuePage'))
const OperationPage = lazy(() => import('./features/meal/pages/ManagerPages').then((module) => ({ default: module.OperationPage })))
const ManagementPeriodPage = lazy(() => import('./features/meal/pages/ManagementPeriodPage'))
const MenuManagementPage = lazy(() => import('./features/meal/pages/MenuManagementPage'))
const PriceManagementPage = lazy(() => import('./features/meal/pages/PriceManagementPage'))
const PurchasesPage = lazy(() => import('./features/meal/pages/PurchasePages').then((module) => ({ default: module.PurchasesPage })))
const PurchaseDetailPage = lazy(() => import('./features/meal/pages/PurchasePages').then((module) => ({ default: module.PurchaseDetailPage })))
const SuppliersPage = lazy(() => import('./features/meal/pages/PurchasePages').then((module) => ({ default: module.SuppliersPage })))
const StudyPage = lazy(() => import('./features/study/StudyPage'))
const AutopsyPage = lazy(() => import('./features/autopsy/AutopsyPage'))
const LostFoundPage = lazy(() => import('./features/lostFound/LostFoundPage'))
const MarketplacePage = lazy(() => import('./features/marketplace/MarketplacePage'))
const ProductDetailsPage = lazy(() => import('./features/marketplace/ProductDetailsPage'))
const WantedPostsPage = lazy(() => import('./features/marketplace/WantedPostsPage'))
const SellerProfilePage = lazy(() => import('./features/marketplace/SellerProfilePage'))
const MarketplaceDashboardPage = lazy(() => import('./features/marketplace/MarketplaceDashboardPage'))
const MarketplaceSafetyPage = lazy(() => import('./features/marketplace/MarketplaceSafetyPage'))
const SellItemPage = lazy(() => import('./features/marketplace/MarketplaceForms').then((module) => ({ default: module.SellItemPage })))
const CreateWantedPostPage = lazy(() => import('./features/marketplace/MarketplaceForms').then((module) => ({ default: module.CreateWantedPostPage })))
const CurrentAffairsPage = lazy(() => import('./features/currentAffairs/CurrentAffairsPage'))
const CurrentAffairsArticlePage = lazy(() => import('./features/currentAffairs/CurrentAffairsArticlePage'))
const CurrentAffairsBookmarksPage = lazy(() => import('./features/currentAffairs/CurrentAffairsLibraryPages').then((module) => ({ default: module.CurrentAffairsBookmarksPage })))
const CurrentAffairsHistoryPage = lazy(() => import('./features/currentAffairs/CurrentAffairsLibraryPages').then((module) => ({ default: module.CurrentAffairsHistoryPage })))
const SignInPage = lazy(() => import('./features/public/AuthPages').then((module) => ({ default: module.SignInPage })))
const CreateAccountPage = lazy(() => import('./features/public/AuthPages').then((module) => ({ default: module.CreateAccountPage })))
const VerifyEmailPage = lazy(() => import('./features/public/AuthPages').then((module) => ({ default: module.VerifyEmailPage })))
const ForgotPasswordPage = lazy(() => import('./features/public/AuthPages').then((module) => ({ default: module.ForgotPasswordPage })))
const ResetPasswordPage = lazy(() => import('./features/public/AuthPages').then((module) => ({ default: module.ResetPasswordPage })))
const OAuthCallbackPage = lazy(() => import('./features/public/AuthPages').then((module) => ({ default: module.OAuthCallbackPage })))
const CompleteProfilePage = lazy(() => import('./features/public/AuthPages').then((module) => ({ default: module.CompleteProfilePage })))
const ProfilePage = lazy(() => import('./features/profile/ProfilePage'))
const MealManagerReviewPage = lazy(() => import('./features/profile/MealManagerReviewPage'))

function FinalYearOnly({ children }: { children: ReactNode }) {
  const { user } = useDemoUser()
  return user.context === 'final-year' ? children : <Navigate to="/meal" replace />
}

function YearsOneToFourOnly({ children }: { children: ReactNode }) {
  const { user } = useDemoUser()
  return user.context !== 'final-year' ? children : <Navigate to="/meal" replace />
}

function ManagerOnly({ children }: { children: ReactNode }) {
  const { user } = useDemoUser()
  return user.isMealManager ? children : <Navigate to="/meal" replace />
}

function LoadingRoute() { return <div className="page-enter"><Skeleton lines={9} /></div> }
function PublicLoadingRoute() { return <div className="cd-public-loading" aria-label="Loading page"><span /><span /><span /></div> }

function ProfileUnavailable() {
  const { refreshProfile } = useDemoUser()
  return <main className="main-content"><div className="content-wrap page-enter">
    <EmptyState title="Profile temporarily unavailable" description="Campus Day could not load your student profile. Check the database connection, then try again." />
    <div className="page-actions"><Button onClick={() => void refreshProfile()}>Try again</Button></div>
  </div></main>
}

function CampusApp() {
  const { authenticated, profileLoading, profileReady } = useDemoUser()
  const session = readAuthSession()
  if (authenticated && session && !session.account.profileComplete) {
    return <Navigate to="/complete-profile" replace />
  }
  if (authenticated && profileLoading) return <PublicLoadingRoute />
  if (authenticated && !profileReady) return <ProfileUnavailable />
  return <AppShell><Suspense fallback={<LoadingRoute />}><Routes>
    <Route path="/study" element={<StudyPage />} />
    <Route path="/autopsy" element={<AutopsyPage />} />
    <Route path="/lost-found" element={<LostFoundPage />} />
    <Route path="/marketplace" element={<MarketplacePage />} />
    <Route path="/marketplace/listings/:listingId" element={<ProductDetailsPage />} />
    <Route path="/marketplace/sell" element={<SellItemPage />} />
    <Route path="/marketplace/wanted" element={<WantedPostsPage />} />
    <Route path="/marketplace/wanted/create" element={<CreateWantedPostPage />} />
    <Route path="/marketplace/sellers/:sellerId" element={<SellerProfilePage />} />
    <Route path="/marketplace/me" element={<MarketplaceDashboardPage />} />
    <Route path="/marketplace/safety" element={<MarketplaceSafetyPage />} />
    <Route path="/current-affairs" element={<CurrentAffairsPage />} />
    <Route path="/current-affairs/bookmarks" element={<CurrentAffairsBookmarksPage />} />
    <Route path="/current-affairs/history" element={<CurrentAffairsHistoryPage />} />
    <Route path="/current-affairs/:articleId" element={<CurrentAffairsArticlePage />} />
    <Route path="/profile" element={<ProfilePage />} />
    <Route path="/meal" element={<DashboardPage />} />
    <Route path="/meal/menu" element={<MenuPage />} />
    <Route path="/meal/absence" element={<YearsOneToFourOnly><AbsencePage /></YearsOneToFourOnly>} />
    <Route path="/meal/order" element={<FinalYearOnly><OrderPage /></FinalYearOnly>} />
    <Route path="/meal/orders" element={<FinalYearOnly><OrdersPage /></FinalYearOnly>} />
    <Route path="/meal/orders/:orderId" element={<FinalYearOnly><OrderDetailPage /></FinalYearOnly>} />
    <Route path="/meal/bills" element={<FinalYearOnly><BillsPage /></FinalYearOnly>} />
    <Route path="/meal/manage" element={<ManagerOnly><ManagerDashboardPage /></ManagerOnly>} />
    <Route path="/meal/manage/period" element={<ManagerOnly><ManagementPeriodPage /></ManagerOnly>} />
    <Route path="/meal/manage/menu" element={<ManagerOnly><MenuManagementPage /></ManagerOnly>} />
    <Route path="/meal/manage/prices" element={<ManagerOnly><PriceManagementPage /></ManagerOnly>} />
    <Route path="/meal/manage/orders" element={<ManagerOnly><OperationPage kind="orders" /></ManagerOnly>} />
    <Route path="/meal/manage/fraction" element={<ManagerOnly><FractionSheetPage /></ManagerOnly>} />
    <Route path="/meal/manage/cooking-plan" element={<ManagerOnly><CookingPlanPage /></ManagerOnly>} />
    <Route path="/meal/manage/stock" element={<ManagerOnly><StockPage /></ManagerOnly>} />
    <Route path="/meal/manage/kitchen-issue" element={<ManagerOnly><KitchenIssuePage /></ManagerOnly>} />
    <Route path="/meal/manage/purchases" element={<ManagerOnly><PurchasesPage /></ManagerOnly>} />
    <Route path="/meal/manage/purchases/:purchaseId" element={<ManagerOnly><PurchaseDetailPage /></ManagerOnly>} />
    <Route path="/meal/manage/suppliers" element={<ManagerOnly><SuppliersPage /></ManagerOnly>} />
    <Route path="/meal/manage/collections" element={<ManagerOnly><FinalYearCollectionsPage /></ManagerOnly>} />
    <Route path="/meal/manage/handover" element={<ManagerOnly><StockHandoverPage /></ManagerOnly>} />
    <Route path="/meal/manage/reports" element={<ManagerOnly><MonthlyReportsPage /></ManagerOnly>} />
    <Route path="*" element={<EmptyState title="Page not found" description="The page you requested does not exist." />} />
  </Routes></Suspense></AppShell>
}

export default function App() {
  return <Suspense fallback={<PublicLoadingRoute />}><Routes>
    <Route path="/" element={<LandingPage />} />
    <Route path="/signin" element={<SignInPage />} />
    <Route path="/create-account" element={<CreateAccountPage />} />
    <Route path="/verify-email" element={<VerifyEmailPage />} />
    <Route path="/forgot-password" element={<ForgotPasswordPage />} />
    <Route path="/reset-password" element={<ResetPasswordPage />} />
    <Route path="/auth/callback" element={<OAuthCallbackPage />} />
    <Route path="/complete-profile" element={<CompleteProfilePage />} />
    <Route path="/meal-manager/review" element={<MealManagerReviewPage />} />
    <Route path="/*" element={<CampusApp />} />
  </Routes></Suspense>
}
