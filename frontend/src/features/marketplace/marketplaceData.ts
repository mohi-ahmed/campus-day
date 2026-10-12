import type { LucideIcon } from 'lucide-react'
import {
  Backpack,
  Bike,
  BookOpen,
  CircleEllipsis,
  Gamepad2,
  Headphones,
  Home,
  Palette,
  Shirt,
  Smartphone,
  Sofa,
} from 'lucide-react'

export type MarketplaceCategory = {
  label: string
  icon: LucideIcon
}

export type Listing = {
  id: string | number
  title: string
  price: number
  category: string
  condition: 'New' | 'Like new' | 'Good' | 'Fair'
  location: string
  posted: string
  seller: string
  sellerId: string
  negotiable: boolean
  image: 'bike' | 'makers' | 'sprite' | 'remote'
  imageUrl?: string
  sprite?: string
  popularity: number
  description: string
  quantity: number
  soldOut?: boolean
}

export type WantedPost = {
  id: string | number
  title: string
  budget: string
  location: string
  responses: number
  condition: string
  author: string
  posted: string
  description: string
}

// Sample UI data. Replace these collections with marketplace API responses during backend integration.
export const ads = [
  {
    image: '/marketplace/campus-bike.png',
    label: 'Featured ride',
    title: 'Move around campus for less',
    copy: 'A dependable bicycle, an easy pickup, and more money left for the semester.',
    action: 'Explore bicycles',
  },
  {
    image: '/marketplace/student-makers.png',
    label: 'Student business',
    title: 'Made here, by students',
    copy: 'Discover notebooks, art, accessories and small-batch pieces from campus creators.',
    action: 'Meet the makers',
  },
  {
    image: '/marketplace/product-grid.png',
    label: 'Fresh this week',
    title: 'Study essentials, ready nearby',
    copy: 'Find the gear you need without waiting for delivery or leaving campus.',
    action: 'Browse fresh finds',
  },
]

export const categories: MarketplaceCategory[] = [
  { label: 'Books & Study', icon: BookOpen },
  { label: 'Electronics', icon: Headphones },
  { label: 'Mobile', icon: Smartphone },
  { label: 'Clothing', icon: Shirt },
  { label: 'Room / Hostel', icon: Home },
  { label: 'Furniture', icon: Sofa },
  { label: 'Bicycle', icon: Bike },
  { label: 'Sports', icon: Gamepad2 },
  { label: 'Bags', icon: Backpack },
  { label: 'Art & Crafts', icon: Palette },
  { label: 'Other', icon: CircleEllipsis },
]

export const listings: Listing[] = [
  {
    id: 1,
    title: 'Casio Scientific Calculator',
    price: 1250,
    category: 'Books & Study',
    condition: 'Like new',
    location: 'CSE Building',
    posted: '2 hours ago',
    seller: 'Nabila Rahman',
    sellerId: 'nabila-rahman',
    negotiable: true,
    image: 'sprite',
    sprite: 'sprite-c0-r0',
    popularity: 92,
    description: 'Carefully used Casio scientific calculator in excellent condition. All buttons work properly and the protective cover is included.',
    quantity: 1,
    soldOut: true,
  },
  {
    id: 2,
    title: 'Adjustable Study Lamp',
    price: 850,
    category: 'Furniture',
    condition: 'Good',
    location: 'Hall Lobby',
    posted: '5 hours ago',
    seller: 'Sadman Sakib',
    sellerId: 'sadman-sakib',
    negotiable: false,
    image: 'sprite',
    sprite: 'sprite-c1-r0',
    popularity: 74,
    description: 'Compact LED desk lamp with three brightness levels and an adjustable neck. Ideal for late-night study sessions.',
    quantity: 1,
    soldOut: true,
  },
  {
    id: 3,
    title: 'Physics & Math Textbook Set',
    price: 1800,
    category: 'Books & Study',
    condition: 'Good',
    location: 'Main Campus',
    posted: '1 day ago',
    seller: 'Tanjim Hossain',
    sellerId: 'tanjim-hossain',
    negotiable: true,
    image: 'sprite',
    sprite: 'sprite-c2-r0',
    popularity: 87,
    description: 'A useful set of first-year physics and mathematics textbooks. A few chapters have light pencil notes, but all pages are intact.',
    quantity: 1,
    soldOut: true,
  },
  {
    id: 4,
    title: 'Campus Commuter Bicycle',
    price: 6500,
    category: 'Bicycle',
    condition: 'Good',
    location: 'Main Gate',
    posted: '1 day ago',
    seller: 'Farzana Ahmed',
    sellerId: 'farzana-ahmed',
    negotiable: true,
    image: 'bike',
    popularity: 98,
    description: 'Comfortable commuter bicycle with a rear carrier, working light and recently serviced brakes. Great for moving between campus buildings.',
    quantity: 1,
    soldOut: true,
  },
  {
    id: 5,
    title: 'Wireless Over-Ear Headphones',
    price: 2200,
    category: 'Electronics',
    condition: 'Like new',
    location: 'Central Square',
    posted: '2 days ago',
    seller: 'Rifat Karim',
    sellerId: 'rifat-karim',
    negotiable: false,
    image: 'sprite',
    sprite: 'sprite-c0-r1',
    popularity: 83,
    description: 'Comfortable wireless headphones with clear sound and reliable battery life. Charging cable and soft carrying pouch are included.',
    quantity: 1,
    soldOut: true,
  },
  {
    id: 6,
    title: 'Everyday Canvas Backpack',
    price: 1350,
    category: 'Bags',
    condition: 'Good',
    location: 'Library',
    posted: '3 days ago',
    seller: 'Mehjabin Noor',
    sellerId: 'mehjabin-noor',
    negotiable: true,
    image: 'sprite',
    sprite: 'sprite-c1-r1',
    popularity: 79,
    description: 'Durable canvas backpack with a padded laptop sleeve, front pocket and two bottle holders. Clean and ready to use.',
    quantity: 1,
    soldOut: true,
  },
  {
    id: 7,
    title: 'Handmade Study Notebooks',
    price: 320,
    category: 'Art & Crafts',
    condition: 'New',
    location: 'Student Centre',
    posted: '3 days ago',
    seller: 'Anika Tasnim',
    sellerId: 'anika-tasnim',
    negotiable: false,
    image: 'sprite',
    sprite: 'sprite-c2-r1',
    popularity: 90,
    description: 'Hand-bound notebooks made by a campus creator. Each notebook has smooth pages and a durable botanical cover.',
    quantity: 8,
    soldOut: true,
  },
  {
    id: 8,
    title: 'Botanical Campus Tote',
    price: 480,
    category: 'Art & Crafts',
    condition: 'New',
    location: 'Cafeteria',
    posted: '4 days ago',
    seller: 'Sanjida Islam',
    sellerId: 'sanjida-islam',
    negotiable: false,
    image: 'makers',
    popularity: 72,
    description: 'Reusable hand-painted canvas tote with a reinforced handle. Made in small batches by a student-run creative shop.',
    quantity: 5,
    soldOut: true,
  },
]

export const wantedPosts: WantedPost[] = [
  { id: 1, title: 'Scientific calculator', budget: 'Up to ৳1,500', location: 'CSE Building', responses: 4, condition: 'Used is fine', author: 'Arafat Hasan', posted: '35 minutes ago', description: 'Looking for a working calculator suitable for engineering courses. A cover would be helpful.' },
  { id: 2, title: 'First-year economics books', budget: 'Up to ৳2,200', location: 'Library', responses: 2, condition: 'Good or better', author: 'Maliha Chowdhury', posted: '2 hours ago', description: 'Need the complete first-year economics book set. Light notes and highlighting are fine.' },
  { id: 3, title: 'Single room study table', budget: 'Up to ৳1,800', location: 'Hall Lobby', responses: 6, condition: 'Any usable condition', author: 'Siam Mahmud', posted: '5 hours ago', description: 'A compact study table that fits inside a hall room. I can collect it this week.' },
  { id: 4, title: 'Android phone for testing', budget: 'Up to ৳4,500', location: 'CSE Building', responses: 1, condition: 'Fair is fine', author: 'Nusrat Jahan', posted: '1 day ago', description: 'Need a basic Android phone for app testing. Battery condition is not very important.' },
  { id: 5, title: 'Badminton racket pair', budget: 'Up to ৳1,200', location: 'Sports Ground', responses: 3, condition: 'Good', author: 'Rakibul Islam', posted: '1 day ago', description: 'Looking for two playable badminton rackets. A set with covers would be preferred.' },
]

export function money(value: number) {
  return `৳${new Intl.NumberFormat('en-BD').format(value)}`
}

export function listingImageSource(listing: Listing) {
  if (listing.image === 'remote' && listing.imageUrl) return listing.imageUrl
  if (listing.image === 'bike') return '/marketplace/campus-bike.png'
  if (listing.image === 'makers') return '/marketplace/student-makers.png'
  return '/marketplace/product-grid.png'
}
