export const MOVIE_TRANSITIONS = {
  DRAFT: [
    {
      target: 'UPCOMING',
      label: 'Duyệt phim',
      variant: 'primary',
      requiresPublishChecklist: true,
      confirmTitle: 'Duyệt phim?',
      confirmDescription: 'Phim sẽ được chuyển sang trạng thái Sắp chiếu.'
    },
    {
      target: 'INACTIVE',
      label: 'Không đưa vào khai thác',
      variant: 'danger',
      requiresPublishChecklist: false,
      confirmTitle: 'Không đưa phim vào khai thác?',
      confirmDescription: 'Phim sẽ rời hàng chờ duyệt và chuyển sang Không hoạt động. Đây không phải trạng thái từ chối có lịch sử riêng.'
    }
  ],
  UPCOMING: [
    {
      target: 'NOW_SHOWING',
      label: 'Bắt đầu công chiếu',
      variant: 'primary',
      requiresPublishChecklist: true,
      confirmTitle: 'Bắt đầu công chiếu phim?',
      confirmDescription: 'Sau khi chuyển trạng thái, phim sẽ được hiển thị là Đang chiếu.'
    },
    {
      target: 'INACTIVE',
      label: 'Chuyển sang Không hoạt động',
      variant: 'danger',
      requiresPublishChecklist: false,
      confirmTitle: 'Chuyển phim sang Không hoạt động?',
      confirmDescription: 'Phim có thể không còn hiển thị trong các luồng vận hành thông thường.'
    }
  ],
  NOW_SHOWING: [
    {
      target: 'ENDED',
      label: 'Kết thúc chiếu',
      variant: 'primary',
      requiresPublishChecklist: false,
      confirmTitle: 'Kết thúc chiếu phim?',
      confirmDescription: 'Trạng thái phim sẽ chuyển sang Đã kết thúc. Các suất chiếu hiện có không tự động bị xóa.'
    },
    {
      target: 'INACTIVE',
      label: 'Chuyển sang Không hoạt động',
      variant: 'danger',
      requiresPublishChecklist: false,
      confirmTitle: 'Chuyển phim sang Không hoạt động?',
      confirmDescription: 'Phim có thể không còn hiển thị trong các luồng vận hành thông thường.'
    }
  ],
  ENDED: [
    {
      target: 'INACTIVE',
      label: 'Chuyển sang Không hoạt động',
      variant: 'danger',
      requiresPublishChecklist: false,
      confirmTitle: 'Chuyển phim sang Không hoạt động?',
      confirmDescription: 'Phim có thể không còn hiển thị trong các luồng vận hành thông thường.'
    },
    {
      target: 'UPCOMING',
      label: 'Chuyển lại Sắp chiếu',
      variant: 'warning',
      requiresPublishChecklist: true,
      confirmTitle: 'Khôi phục phim về Sắp chiếu?',
      confirmDescription: 'Phim sẽ xuất hiện trở lại trong nhóm Sắp chiếu. Hãy chắc chắn trước khi thực hiện.'
    }
  ],
  INACTIVE: [
    {
      target: 'DRAFT',
      label: 'Khôi phục về Chờ duyệt',
      variant: 'secondary',
      requiresPublishChecklist: false,
      confirmTitle: 'Khôi phục phim về Chờ duyệt?',
      confirmDescription: 'Phim sẽ được đưa về trạng thái kiểm duyệt ban đầu.'
    }
  ]
};
