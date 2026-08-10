export const MOVIE_TRANSITIONS = {
  DRAFT: [
    {
      target: 'UPCOMING',
      label: 'Duyệt sang Sắp chiếu',
      variant: 'primary',
      requiresPublishChecklist: true,
      confirmTitle: 'Duyệt phim sang Sắp chiếu?',
      confirmDescription: 'Phim sẽ chuyển sang Sắp chiếu. Ngày bắt đầu khai thác phải sau hôm nay.'
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
      label: 'Đưa vào đợt chiếu lại',
      variant: 'warning',
      requiresPublishChecklist: true,
      confirmTitle: 'Đưa phim vào đợt chiếu lại?',
      confirmDescription: 'Phim sẽ xuất hiện trở lại trong nhóm Sắp chiếu theo đợt khai thác mới. Hãy lập đợt khai thác trước khi thực hiện.'
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
