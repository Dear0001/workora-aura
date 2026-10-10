export const MAX_TASK_FILE_SIZE = 25 * 1024 * 1024;
export const TASK_FILE_ACCEPT = 'image/*,.pdf,.txt,.log,.mp4,.mov,.zip';

const TASK_FILE_EXTENSIONS = new Set([
  'jpg',
  'jpeg',
  'png',
  'gif',
  'webp',
  'svg',
  'bmp',
  'tif',
  'tiff',
  'avif',
  'heic',
  'pdf',
  'txt',
  'log',
  'mp4',
  'mov',
  'zip',
]);

export function getTaskFileValidationError(file: File): string | null {
  const extension = file.name.split('.').pop()?.toLowerCase() ?? '';
  if (!TASK_FILE_EXTENSIONS.has(extension)) {
    return 'unsupported file type';
  }
  if (file.size > MAX_TASK_FILE_SIZE) {
    return 'file must be 25 MB or smaller';
  }
  return null;
}
