import { post } from './apiClient';
import type { PageResult } from '../types/api';

export type Shop = {
  id: number; ownerUsername: string; name: string; coverUrl?: string | null;
  description: string; contact?: string; status: 'ACTIVE' | 'BLOCKED'; productCount?: number;
};
export type Product = {
  id: number; shopId: number; title: string; category: string; coverUrl?: string | null;
  summary: string; content?: string; price: string | number; stock: number; sortOrder: number;
  status: 'DRAFT' | 'PUBLISHED' | 'OFFLINE'; moderationStatus: 'ACTIVE' | 'BLOCKED'; moderationReason?: string | null;
  shopName?: string; contact?: string; publishedAt?: string | null;
};
export type ProductInput = Record<string, string>;
const root = '/api/users/commerce';
export function loadShops(keyword = '', pageNum = 1) { return post<PageResult<Shop>>(`${root}/shops/page`, { body: { keyword, pageNum, pageSize: 12 } }); }
export function loadShop(id: number) { return post<Shop>(`${root}/shops/${id}`); }
export function loadProducts(shopId: number, keyword = '', category = '', pageNum = 1) { return post<PageResult<Product>>(`${root}/shops/${shopId}/products/page`, { body: { keyword, category, pageNum, pageSize: 12 } }); }
export function loadProductCategories(shopId: number) { return post<string[]>(`${root}/shops/${shopId}/categories`); }
export function loadProduct(id: number) { return post<Product>(`${root}/products/detail/${id}`); }
export function loadMyShop() { return post<Shop | null>(`${root}/mine/shop`); }
export function saveMyShop(body: Record<string, string>) { return post<Shop>(`${root}/mine/shop/save`, { body }); }
export function loadMyProducts(pageNum = 1, keyword = '', status = '') { return post<PageResult<Product>>(`${root}/mine/products/page`, { body: { pageNum, pageSize: 12, keyword, status } }); }
export function saveMyProduct(id: number | null, body: ProductInput) { return post<void>(id === null ? `${root}/mine/products` : `${root}/mine/products/update/${id}`, { body }); }
export function deleteMyProduct(id: number) { return post<void>(`${root}/mine/products/delete/${id}`); }
