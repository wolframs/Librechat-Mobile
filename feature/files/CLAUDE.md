# feature:files

## Screen
`FilesScreen` -- sealed interface `FilesRoute : NavKey` with single route `Files` (`@Serializable` data object). Lists uploaded files with upload and delete actions.

## ViewModel
`FilesViewModel` depends on `FileRepository` and application `Context` (for content resolver).

### State
`FilesUiState`: `files: List<FileObject>`, `isLoading`, `isRefreshing`, `isUploading`, `error`

### Operations
| Action | Method | Notes |
|--------|--------|-------|
| Load | `loadFiles()` | `FileRepository.getFiles()` |
| Refresh | `refresh()` | Same endpoint, sets `isRefreshing` |
| Upload | `uploadFile(uri)` | Opens a reopenable streaming source, checks the known server limit, then calls `FileRepository.uploadFile(source, filename, mimeType)` |
| Delete | `deleteFile(fileId)` | `FileRepository.deleteFiles(listOf(fileId))`, removes from local list |

## Upload Flow
1. User picks file via SAF (`ActivityResultContracts.GetContent`)
2. `ContentResolver` resolves filename, MIME type, and optional size without reading the body
3. The known endpoint size limit is enforced before transfer
4. Multipart upload via `FilesApi` streams a fresh channel from the platform source (and can
   reopen it if Ktor replays the request)
5. New file prepended to list on success

## Image Viewing
- `FullscreenImageViewer` (in `feature:chat/components/`) provides pinch-to-zoom and pan
- Image URLs: `/api/files/download/:userId/:file_id` for server files
- Loaded via Coil `AsyncImage` with placeholder and error states

## Data Layer
- `FileRepository` in `:core:data` wraps `FilesApi` from `:core:network`
- API endpoints: `GET /api/files` (list), `POST /api/files` (upload, multipart), `DELETE /api/files` (batch delete)

## Spec Notes (not yet implemented)
- Multi-select mode for bulk deletion
- File preview on tap (non-image files)

### File Type Filtering
- `FilesScreen` renders an inline `ScrollableTabRow` (All, Images, Documents, Audio, Video)
- Filters by MIME type prefix in `FilesViewModel` (`FileTypeFilter`)

### Upload Progress
- `UploadProgressCard` — animated card with filename, LinearProgressIndicator, cancel button
- Replaces the old fullscreen CircularProgressIndicator overlay
- `FilesViewModel` tracks `uploadProgress` and `uploadFilename` in state
- Cancel via stored upload Job reference; cancellation closes the active multipart channel
