import { Component } from 'react';
import { ServerErrorPage } from '@/features/auth/pages/ErrorPages';

export default class AppErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { failed: false };
  }

  static getDerivedStateFromError() {
    return { failed: true };
  }

  componentDidCatch(error, errorInfo) {
    console.error('Uncaught application render error', error, errorInfo);
  }

  retry = () => {
    this.setState({ failed: false });
    window.location.reload();
  };

  render() {
    if (this.state.failed) {
      return <ServerErrorPage onRetry={this.retry} />;
    }
    return this.props.children;
  }
}
