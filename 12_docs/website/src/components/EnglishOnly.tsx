import React from 'react';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Admonition from '@theme/Admonition';
import Translate from '@docusaurus/Translate';

export default function EnglishOnly(): React.ReactNode {
  const {i18n} = useDocusaurusContext();
  if (i18n.currentLocale === i18n.defaultLocale) {
    return null;
  }
  return (
    <Admonition type="note">
      <Translate id="englishOnly.note">This page is available in English only.</Translate>
    </Admonition>
  );
}
