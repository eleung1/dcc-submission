export const defaultTableOptions = {
  sizePerPage: 20,
  paginationShowsTotal: (start, to, total) => `Showing ${start + 1} to ${Math.min(to + 1, total)} of ${total} entries`,
  hideSizePerPage: true,
  defaultSortOrder: 'asc',
  thresholdToShowSearch: 10,
};

export const defaultTableProps = {
  searchPlaceholder: 'Table filter'
};

export default defaultTableOptions;