import { apiRequest } from '../../lib/api'
import type { Listing, WantedPost } from './marketplaceData'

export type MarketplaceCategoryApi = 'BOOKS_AND_STUDY' | 'ELECTRONICS' | 'MOBILE_AND_ACCESSORIES' | 'CLOTHING' | 'ROOM_AND_HOSTEL' | 'FURNITURE' | 'BICYCLE' | 'SPORTS' | 'BAGS_AND_ACCESSORIES' | 'HANDMADE_AND_STUDENT_BUSINESS' | 'ART_AND_CRAFTS' | 'OTHER'
export type MarketplaceConditionApi = 'NEW' | 'LIKE_NEW' | 'USED_GOOD' | 'USED_FAIR'
export type MarketplaceListingTypeApi = 'USED' | 'STUDENT_BUSINESS'
export type MarketplacePickupZoneApi = 'MAIN_GATE' | 'ACADEMIC_BUILDING' | 'CSE_BUILDING' | 'LIBRARY' | 'HALL_LOBBY' | 'CAFETERIA' | 'STUDENT_CENTRE' | 'OTHER_PUBLIC_CAMPUS_AREA'

export interface MarketplaceImageApi {
  id: string
  secureUrl: string
  format: string
  width: number
  height: number
  bytes: number
  sortOrder: number
  createdAt: string
}

export interface MarketplaceListingApi {
  id: string
  sellerStudentId: string
  sellerName: string
  title: string
  description: string
  category: MarketplaceCategoryApi
  price: number
  condition: MarketplaceConditionApi
  listingType: MarketplaceListingTypeApi
  quantity: number
  negotiable: boolean
  pickupZone: MarketplacePickupZoneApi
  status: 'AVAILABLE' | 'RESERVED' | 'SOLD' | 'UNAVAILABLE'
  images: MarketplaceImageApi[]
  viewCount: number
  createdAt: string
  updatedAt: string
}

export interface WantedPostApi {
  id: string
  authorStudentId: string
  authorName: string
  title: string
  description: string
  category: MarketplaceCategoryApi
  maximumBudget: number
  acceptableCondition: MarketplaceConditionApi
  pickupZone: MarketplacePickupZoneApi
  status: 'OPEN' | 'FULFILLED' | 'CLOSED'
  responseCount: number
  createdAt: string
  updatedAt: string
}

export interface MarketplaceOfferApi {
  id: string
  listingId: string
  listingTitle: string
  buyerStudentId: string
  buyerName: string
  sellerStudentId: string
  offeredAmount: number
  counterAmount?: number
  message?: string
  status: 'PENDING' | 'COUNTERED' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED'
  createdAt: string
  updatedAt: string
}

interface MarketplaceFavoriteApi {
  studentId: string
  items: MarketplaceListingApi[]
}

interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface CreateListingInput {
  sellerStudentId: string
  title: string
  description: string
  category: MarketplaceCategoryApi
  price: number
  condition: MarketplaceConditionApi
  listingType: MarketplaceListingTypeApi
  quantity: number
  negotiable: boolean
  pickupZone: MarketplacePickupZoneApi
  imageIds: string[]
}

export interface CreateWantedPostInput {
  authorStudentId: string
  title: string
  description: string
  category: MarketplaceCategoryApi
  maximumBudget: number
  acceptableCondition: MarketplaceConditionApi
  pickupZone: MarketplacePickupZoneApi
}

const base = '/api/marketplace'

function queryString(values: Record<string, string | number | boolean | undefined>) {
  const params = new URLSearchParams()
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== '') params.set(key, String(value))
  })
  const query = params.toString()
  return query ? `?${query}` : ''
}

export const marketplaceApi = {
  listListings: (query?: string) => apiRequest<PageResponse<MarketplaceListingApi>>(
    `${base}/listings${queryString({ query: query?.trim(), page: 0, size: 50 })}`,
    undefined,
    'Marketplace listings',
  ),

  getListing: (listingId: string) => apiRequest<MarketplaceListingApi>(
    `${base}/listings/${listingId}`,
    undefined,
    'Marketplace listing',
  ),

  listSellerListings: (studentId: string) => apiRequest<PageResponse<MarketplaceListingApi>>(
    `${base}/listings/seller/${encodeURIComponent(studentId)}${queryString({ page: 0, size: 50 })}`,
    undefined,
    'Seller marketplace listings',
  ),

  uploadImage: (studentId: string, file: File) => {
    const body = new FormData()
    body.append('studentId', studentId)
    body.append('file', file)
    return apiRequest<MarketplaceImageApi>(`${base}/images`, { method: 'POST', body }, 'Upload marketplace image')
  },

  createListing: (input: CreateListingInput) => apiRequest<MarketplaceListingApi>(
    `${base}/listings`,
    { method: 'POST', body: JSON.stringify(input) },
    'Create marketplace listing',
  ),

  listWantedPosts: (query?: string) => apiRequest<PageResponse<WantedPostApi>>(
    `${base}/wanted-posts${queryString({ query: query?.trim(), page: 0, size: 50 })}`,
    undefined,
    'Wanted posts',
  ),

  createWantedPost: (input: CreateWantedPostInput) => apiRequest<WantedPostApi>(
    `${base}/wanted-posts`,
    { method: 'POST', body: JSON.stringify(input) },
    'Create wanted post',
  ),

  respondToWantedPost: (postId: string, input: { sellerStudentId: string; listingId?: string; price: number; message: string }) =>
    apiRequest(`${base}/wanted-posts/${postId}/responses`, {
      method: 'POST',
      body: JSON.stringify(input),
    }, 'Respond to wanted post'),

  makeOffer: (listingId: string, input: { buyerStudentId: string; amount: number; message?: string }) =>
    apiRequest(`${base}/listings/${listingId}/offers`, {
      method: 'POST',
      body: JSON.stringify(input),
    }, 'Make marketplace offer'),

  sendMessage: (input: { senderStudentId: string; recipientStudentId: string; listingId?: string; body: string }) =>
    apiRequest(`${base}/messages`, {
      method: 'POST',
      body: JSON.stringify(input),
    }, 'Send marketplace message'),

  saveListing: (listingId: string, studentId: string) => apiRequest<void>(
    `${base}/favorites/${listingId}`,
    { method: 'POST', body: JSON.stringify({ studentId }) },
    'Save marketplace listing',
  ),

  removeSavedListing: (listingId: string, studentId: string) => apiRequest<void>(
    `${base}/favorites/${listingId}${queryString({ studentId })}`,
    { method: 'DELETE' },
    'Remove saved marketplace listing',
  ),

  listSavedListings: (studentId: string) => apiRequest<MarketplaceFavoriteApi>(
    `${base}/favorites${queryString({ studentId })}`,
    undefined,
    'Saved marketplace listings',
  ),

  listReceivedOffers: (sellerStudentId: string) => apiRequest<MarketplaceOfferApi[]>(
    `${base}/offers/received${queryString({ sellerStudentId })}`,
    undefined,
    'Received marketplace offers',
  ),

  actOnOffer: (offerId: string, action: 'accept' | 'reject' | 'counter', actorStudentId: string, counterAmount?: number) =>
    apiRequest<MarketplaceOfferApi>(`${base}/offers/${offerId}/${action}`, {
      method: 'PATCH',
      body: JSON.stringify({ actorStudentId, counterAmount }),
    }, `${action} marketplace offer`),

  report: (input: { reporterStudentId: string; targetType: 'LISTING' | 'SELLER'; targetId: string; reason: string; details?: string }) =>
    apiRequest(`${base}/reports`, { method: 'POST', body: JSON.stringify(input) }, 'Report marketplace content'),
}

const categoryMap: Record<string, MarketplaceCategoryApi> = {
  'Books & Study': 'BOOKS_AND_STUDY',
  Electronics: 'ELECTRONICS',
  Mobile: 'MOBILE_AND_ACCESSORIES',
  Clothing: 'CLOTHING',
  'Room / Hostel': 'ROOM_AND_HOSTEL',
  Furniture: 'FURNITURE',
  Bicycle: 'BICYCLE',
  Sports: 'SPORTS',
  Bags: 'BAGS_AND_ACCESSORIES',
  'Art & Crafts': 'ART_AND_CRAFTS',
  Other: 'OTHER',
}

const pickupMap: Record<string, MarketplacePickupZoneApi> = {
  'Main Gate': 'MAIN_GATE',
  'Academic Building': 'ACADEMIC_BUILDING',
  'CSE Building': 'CSE_BUILDING',
  Library: 'LIBRARY',
  'Hall Lobby': 'HALL_LOBBY',
  Cafeteria: 'CAFETERIA',
  'Student Centre': 'STUDENT_CENTRE',
}

const conditionMap: Record<string, MarketplaceConditionApi> = {
  New: 'NEW',
  'New only': 'NEW',
  'Like new': 'LIKE_NEW',
  Good: 'USED_GOOD',
  'Good or better': 'USED_GOOD',
  Fair: 'USED_FAIR',
  'Used is fine': 'USED_FAIR',
}

export const marketplaceApiValues = {
  category: (label: string) => categoryMap[label] ?? 'OTHER',
  pickup: (label: string) => pickupMap[label] ?? 'OTHER_PUBLIC_CAMPUS_AREA',
  condition: (label: string) => conditionMap[label] ?? 'USED_FAIR',
}

const categoryLabels: Record<MarketplaceCategoryApi, string> = {
  BOOKS_AND_STUDY: 'Books & Study',
  ELECTRONICS: 'Electronics',
  MOBILE_AND_ACCESSORIES: 'Mobile',
  CLOTHING: 'Clothing',
  ROOM_AND_HOSTEL: 'Room / Hostel',
  FURNITURE: 'Furniture',
  BICYCLE: 'Bicycle',
  SPORTS: 'Sports',
  BAGS_AND_ACCESSORIES: 'Bags',
  HANDMADE_AND_STUDENT_BUSINESS: 'Art & Crafts',
  ART_AND_CRAFTS: 'Art & Crafts',
  OTHER: 'Other',
}

const pickupLabels: Record<MarketplacePickupZoneApi, string> = {
  MAIN_GATE: 'Main Gate',
  ACADEMIC_BUILDING: 'Academic Building',
  CSE_BUILDING: 'CSE Building',
  LIBRARY: 'Library',
  HALL_LOBBY: 'Hall Lobby',
  CAFETERIA: 'Cafeteria',
  STUDENT_CENTRE: 'Student Centre',
  OTHER_PUBLIC_CAMPUS_AREA: 'Campus pickup zone',
}

const conditionLabels: Record<MarketplaceConditionApi, Listing['condition']> = {
  NEW: 'New',
  LIKE_NEW: 'Like new',
  USED_GOOD: 'Good',
  USED_FAIR: 'Fair',
}

export function listingFromApi(item: MarketplaceListingApi): Listing {
  return {
    id: item.id,
    title: item.title,
    price: Number(item.price),
    category: categoryLabels[item.category],
    condition: conditionLabels[item.condition],
    location: pickupLabels[item.pickupZone],
    posted: 'Recently',
    seller: item.sellerName,
    sellerId: item.sellerStudentId,
    negotiable: item.negotiable,
    image: 'remote',
    imageUrl: item.images[0]?.secureUrl,
    popularity: item.viewCount,
    description: item.description,
    quantity: item.quantity,
    soldOut: item.status === 'SOLD' || item.status === 'UNAVAILABLE',
  }
}

export function wantedPostFromApi(item: WantedPostApi): WantedPost {
  return {
    id: item.id,
    title: item.title,
    budget: `Up to ৳${new Intl.NumberFormat('en-BD').format(Number(item.maximumBudget))}`,
    location: pickupLabels[item.pickupZone],
    responses: Number(item.responseCount),
    condition: conditionLabels[item.acceptableCondition],
    author: item.authorName,
    posted: 'Recently',
    description: item.description,
  }
}
