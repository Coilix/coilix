import React from 'react';

type Props = {
  href: string;
  children: React.ReactNode;
};

export default function ApiLink({href, children}: Props): React.ReactNode {
  return <a href={href}>{children}</a>;
}
