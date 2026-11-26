---
title: Talking to Miro boards, live on stage
date: 2026-09-17
slug: miro-ai-voice-mode-canvas-26
summary: At Canvas '26 we showed people talking to their Miro boards, live on four keynote stages. I was an engineer on AI Voice Mode, and we got there from kickoff in about ten weeks.
tags: [it, ai, miro]
---

At [Canvas '26](https://miro.com/newsroom/miro-takes-aim-at-the-gap-between-ai-potential-and-organizational-reality/) — San Francisco, London, Sydney and Tokyo — we showed something I'd been dreaming about for a while: people talking to their Miro boards, live on stage. In Tokyo, it even spoke Japanese.

Now that the dust has settled, I want to take a moment to celebrate it.

I was an engineer on AI Voice Mode at Miro: a real-time, speech-to-speech AI you converse with while you work on the canvas. You think out loud; the board listens, answers and acts. We went from kickoff to the keynote mainstage in about ten weeks.

A few things made this special.

## The engineering

Real-time voice is unforgiving: sub-second voice-to-voice latency, barge-in and interruption handling, audio pipelines, WebRTC. There's nowhere to hide a slow code path when someone is waiting for an answer mid-sentence.

## The AI craft

A voice agent isn't a chatbot with a speaker. It has to sound like Miro, know when to speak and when to stay quiet, transcribe live, and translate spoken intent into tool calls that actually do the work on the board.

## The architecture bet

We split the system in two: a fast voice agent that only converses, and the full agent platform behind it that does the actual work. A receptionist and a knowledge worker. Months later, OpenAI shipped GPT-Live built on the same delegation pattern. The best confirmation we could ask for that we'd read the problem right.

## The collaboration

We built this working closely with the OpenAI team on their Realtime API. Iterating directly with the people building the underlying models made both the product and the tech sharper.

## The team

Kickoff to four keynote stages in one quarter takes people who care about craft under pressure, and I got to build this alongside exactly those people. You know who you are. Thank you.

Building this changed how I think about the future of work: it's not just multiplayer, it's conversational.
