import { useEffect, useLayoutEffect, useRef } from "react";
import { useLocation, useNavigationType } from "react-router-dom";

const SCROLL_POSITION_PREFIX = "lorafilm:scroll-position:";

const readScrollPosition = locationKey => {
  try {
    const value = window.sessionStorage.getItem(`${SCROLL_POSITION_PREFIX}${locationKey}`);
    return value ? JSON.parse(value) : null;
  } catch {
    return null;
  }
};

const writeScrollPosition = (locationKey, position) => {
  try {
    window.sessionStorage.setItem(
      `${SCROLL_POSITION_PREFIX}${locationKey}`,
      JSON.stringify(position),
    );
  } catch {
    // Storage can be unavailable in privacy-restricted browser contexts.
  }
};

export default function ScrollToTop() {
  const { pathname, hash, key } = useLocation();
  const navigationType = useNavigationType();
  const previousPathnameRef = useRef(null);

  useEffect(() => {
    const previousRestoration = window.history.scrollRestoration;
    window.history.scrollRestoration = "manual";
    return () => {
      window.history.scrollRestoration = previousRestoration;
    };
  }, []);

  useEffect(() => {
    let animationFrame = null;
    const savePosition = () => {
      animationFrame = null;
      writeScrollPosition(key, { left: window.scrollX, top: window.scrollY });
    };
    const handleScroll = () => {
      if (animationFrame == null) animationFrame = window.requestAnimationFrame(savePosition);
    };

    window.addEventListener("scroll", handleScroll, { passive: true });
    return () => {
      window.removeEventListener("scroll", handleScroll);
      if (animationFrame != null) window.cancelAnimationFrame(animationFrame);
    };
  }, [key]);

  useLayoutEffect(() => {
    const pathnameChanged = previousPathnameRef.current !== pathname;
    previousPathnameRef.current = pathname;
    if (!pathnameChanged && !hash) return undefined;

    if (hash) {
      const target = document.getElementById(decodeURIComponent(hash.slice(1)));
      if (target) {
        target.scrollIntoView({ behavior: "smooth", block: "start" });
        return undefined;
      }
    }

    const savedPosition = navigationType === "POP" ? readScrollPosition(key) : null;
    const targetPosition = savedPosition || { left: 0, top: 0 };
    let attempts = 0;
    let retryTimer = null;
    let userInterrupted = false;
    const stopRestoring = () => {
      userInterrupted = true;
      if (retryTimer != null) window.clearTimeout(retryTimer);
    };
    const interruptionEvents = ["wheel", "touchstart", "pointerdown", "keydown"];
    interruptionEvents.forEach(eventName => {
      window.addEventListener(eventName, stopRestoring, { passive: true });
    });
    const restorePosition = () => {
      if (userInterrupted) return;
      window.scrollTo({ ...targetPosition, behavior: "auto" });
      attempts += 1;
      if (savedPosition && attempts < 12) {
        retryTimer = window.setTimeout(restorePosition, 50);
      }
    };

    restorePosition();
    return () => {
      if (retryTimer != null) window.clearTimeout(retryTimer);
      interruptionEvents.forEach(eventName => {
        window.removeEventListener(eventName, stopRestoring);
      });
    };
  }, [hash, key, navigationType, pathname]);

  return null;
}
