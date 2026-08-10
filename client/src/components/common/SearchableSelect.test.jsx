import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import SearchableSelect from './SearchableSelect';

const options = [
  { value: 'cinema-a', label: 'Cinema A', subtitle: 'Quận 1' },
  { value: 'cinema-disabled', label: 'Cinema Disabled', disabled: true },
  { value: 'cinema-b', label: 'Cinema B', subtitle: 'Quận 3' },
];

describe('SearchableSelect', () => {
  it('exposes combobox/listbox semantics and selects with Arrow and Enter', () => {
    const onChange = vi.fn();
    render(
      <SearchableSelect
        id="cinema"
        ariaLabel="Cinema"
        options={options}
        value=""
        onChange={onChange}
      />,
    );

    const combobox = screen.getByRole('combobox', { name: 'Cinema' });
    expect(combobox).toHaveAttribute('aria-expanded', 'false');
    fireEvent.keyDown(combobox, { key: 'ArrowDown' });

    expect(combobox).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('listbox', { name: 'Cinema' })).toBeInTheDocument();
    expect(screen.getAllByRole('option')).toHaveLength(3);
    expect(screen.getByRole('option', { name: /Cinema Disabled/i })).toHaveAttribute('aria-disabled', 'true');

    fireEvent.keyDown(combobox, { key: 'Enter' });
    expect(onChange).toHaveBeenCalledWith('cinema-a');
  });

  it('filters by typing, closes with Escape, and exposes an accessible clear action', () => {
    const onChange = vi.fn();
    const { rerender } = render(
      <SearchableSelect ariaLabel="Cinema" options={options} value="cinema-a" onChange={onChange} />,
    );
    const combobox = screen.getByRole('combobox', { name: 'Cinema' });
    expect(combobox).toHaveValue('Cinema A');

    fireEvent.click(combobox);
    fireEvent.change(combobox, { target: { value: 'Quận 3' } });
    expect(screen.getAllByRole('option')).toHaveLength(1);
    fireEvent.keyDown(combobox, { key: 'Enter' });
    expect(onChange).toHaveBeenCalledWith('cinema-b');

    rerender(<SearchableSelect ariaLabel="Cinema" options={options} value="cinema-a" onChange={onChange} />);
    fireEvent.click(screen.getByRole('combobox', { name: 'Cinema' }));
    fireEvent.keyDown(screen.getByRole('combobox', { name: 'Cinema' }), { key: 'Escape' });
    expect(screen.getByRole('combobox', { name: 'Cinema' })).toHaveAttribute('aria-expanded', 'false');

    fireEvent.click(screen.getByRole('button', { name: 'Xóa lựa chọn Cinema A' }));
    expect(onChange).toHaveBeenCalledWith('');
  });
});
